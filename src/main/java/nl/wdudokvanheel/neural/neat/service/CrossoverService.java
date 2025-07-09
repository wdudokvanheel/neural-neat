package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.CreatureInterface;
import nl.wdudokvanheel.neural.neat.NeatContext;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CrossoverService<Creature extends CreatureInterface<Creature>> {
    private Logger logger = LoggerFactory.getLogger(CrossoverService.class);
    private Random random = new Random();

    public List<Creature> createOffspring(NeatContext<Creature> context, int population) {
        ArrayList<Creature> creatures = new ArrayList<>();

        if (population <= 0) {
            return creatures;
        }

        List<Species<Creature>> sorted = context.speciationService.sortSpeciesByScore(context.species);
        double totalFitness = getTotalScore(sorted);

        int speciesCount = sorted.size();
        int[] quota = new int[speciesCount];
        double[] remainder = new double[speciesCount];
        int remaining = population;

        for (int i = 0; i < speciesCount; i++) {
            Species<Creature> s = sorted.get(i);
            double exact = totalFitness == 0 ? 0 : (s.getFitness() / totalFitness) * population;
            quota[i] = (int) Math.floor(exact);
            remainder[i] = exact - quota[i];
            remaining -= quota[i];
        }

        while (remaining > 0) {
            int best = 0;
            for (int i = 1; i < speciesCount; i++) {
                if (remainder[i] > remainder[best]) {
                    best = i;
                }
            }
            quota[best]++;
            remainder[best] = 0;
            remaining--;
        }

        for (int i = 0; i < speciesCount; i++) {
            Species<Creature> species = sorted.get(i);
            int desired = quota[i];
            int asexual = (int) Math.round(desired * context.configuration.reproduceWithoutCrossover);
            int sexual = desired - asexual;

//            logger.debug("Creating {} offspring ({} asexual) for species {}", sexual + asexual, asexual, species);

            creatures.addAll(createOffspringWithoutCrossover(context, species, asexual));
            creatures.addAll(createOffspring(context, species, sexual));
        }

        if (creatures.size() > population) {
            Collections.shuffle(creatures, random);
            creatures.subList(population, creatures.size()).clear();
        }

        while (creatures.size() < population) {
            Species<Creature> fallback = sorted.get(random.nextInt(sorted.size()));
            creatures.addAll(createOffspringWithoutCrossover(context, fallback, 1));
        }

        return creatures;
    }

    private List<Creature> createOffspring(NeatContext<Creature> context, Species<Creature> species, int offspring) {
        List<Creature> creatures = new ArrayList<>();
        if (species.size() == 0) {
            return creatures;
        }

        for (int i = 0; i < offspring; i++) {
            //Get two random creatures
            Creature parentA = selectRandomWeightedCreature(species);
            //Make sure to exclude the other parent
            Species<Creature> parentBSpecies = species;

            if ((random.nextDouble() < context.configuration.interspeciesCrossover && context.species.size() > 1) || species.size() == 1) {
                parentBSpecies = selectRandomWeightedSpecies(context.species, species);
            }
            // If there is no other species available, add an asexual offspring
            if (parentBSpecies == null) {
                creatures.add(createOffspringWithoutCrossover(context, species, 1).getFirst());
                continue;
            }
            Creature parentB = selectRandomWeightedCreature(parentBSpecies, parentA);

            Creature child = crossCreatures(context, parentA, parentB);
            creatures.add(child);
        }
        logger.trace("Created {} offspring with crossover", creatures.size());

        return creatures;
    }

    private Species<Creature> selectRandomWeightedSpecies(List<Species<Creature>> species) {
        return selectRandomWeightedSpecies(species, null);
    }

    private Species<Creature> selectRandomWeightedSpecies(List<Species<Creature>> species, Species<Creature> exclude) {
        double totalFitness = 0;

        for (Species<Creature> iter : species) {
            if (iter == exclude) {
                continue;
            }

            totalFitness += iter.getFitness();
        }

        if (totalFitness <= 0) {
            if (species.get(0) != exclude) {
                return species.getFirst();
            } else if (species.size() > 1) {
                return species.get(1);
            } else {
                return null;
            }
        }

        double target = random.nextDouble(totalFitness);
        double count = 0;
        for (Species<Creature> iter : species) {
            if (iter == exclude) {
                continue;
            }

            count += iter.getFitness();
            if (count >= target) {
                return iter;
            }
        }

        return null;
    }

    private Creature crossCreatures(NeatContext<Creature> context, Creature parentA, Creature parentB) {
        Genome fit = parentA.getGenome();
        Genome weak = parentB.getGenome();

        if (parentB.getFitness() > parentA.getFitness()) {
            fit = parentB.getGenome();
            weak = parentA.getGenome();
        }

        Genome genome = crossover(fit, weak);
        context.mutationService.mutateGenome(genome);
        return context.creatureFactory.createNewCreature(genome);
    }

    private List<Creature> createOffspringWithoutCrossover(NeatContext<Creature> context, Species<Creature> species, int offspring) {
        List<Creature> creatures = new ArrayList<>();

        for (int i = 0; i < offspring; i++) {
            Creature creature = selectRandomCreature(species);
            //Clone a random creature's genome
            Genome genome = creature.getGenome().clone();
            //Mutate the genome
            context.mutationService.mutateGenome(genome);
            //Create a new creature with the specified offspring
            Creature newCreature = context.creatureFactory.createNewCreature(genome);
            creatures.add(newCreature);
        }
        logger.trace("Created {} offspring without crossover", creatures.size());
        return creatures;
    }

    private Creature selectRandomCreature(Species<Creature> species) {
        return species.getCreatures().get(random.nextInt(species.getCreatures().size()));
    }

    private Creature selectRandomCreature(Species<Creature> species, Creature exclude) {
        ArrayList<Creature> list = new ArrayList<>(species.getCreatures());
        list.remove(exclude);
        return list.get(random.nextInt(list.size()));
    }

    private Creature selectRandomCreature(List<Creature> creatures, Creature exclude) {
        ArrayList<Creature> list = new ArrayList<>(creatures);
        list.remove(exclude);
        return list.get(random.nextInt(list.size()));
    }

    private Creature selectRandomWeightedCreature(Species<Creature> species) {
        return selectRandomWeightedCreature(species, null);
    }

    private Creature selectRandomWeightedCreature(Species<Creature> species, Creature exclude) {
        double totalFitness = 0;

        for (Creature creature : species.getCreatures()) {
            if (creature == exclude) {
                continue;
            }

            totalFitness += creature.getFitness();
        }

        if (totalFitness <= 0) {
            if (species.getCreatures().get(0) != exclude) {
                return species.getCreatures().getFirst();
            } else if (species.getCreatures().size() > 1) {
                return species.getCreatures().get(1);
            } else {
                return species.getCreatures().getFirst();
            }
        }
        double target = random.nextDouble(totalFitness);
        double count = 0;
        for (Creature creature : species.getCreatures()) {
            if (creature == exclude) {
                continue;
            }

            count += creature.getFitness();
            if (count >= target) {
                return creature;
            }
        }

        return null;
    }

    private double getTotalScore(List<Species<Creature>> species) {
        double total = 0;
        for (Species<Creature> iter : species) {
            total += iter.getFitness();
        }
        return total;
    }

    /**
     * Cross two genomes into a new child.
     *
     * @param fitParent The fittest parent of the two
     * @param parent    The least fit parent
     * @return A new genome
     */
    public Genome crossover(Genome fitParent, Genome parent) {
        Genome child = new Genome();

        //Add all the neurons from the fit parent, leaving out the disjoint and excess neurons from the less fit parent
        fitParent.getNeurons().forEach(neuron -> child.addNeuron(neuron.clone()));

        //Add all the connections from the fit parent, omitting disjoint and excess connections from the less fit parent
        for (ConnectionGene parentA : fitParent.getConnections()) {
            //Get the connection from the other parent
            ConnectionGene parentB = parent.getConnectionById(parentA.getInnovationId());

            //Clone from the fit parent
            ConnectionGene connection = parentA.clone();

            //If parentB has the connection as well, give it a 50% chance to use its weight as this
            //are the only values that might differ between the parents
            if (parentB != null && random.nextBoolean()) {
                connection.setWeight(parentB.getWeight());
            }

            boolean enabled = parentA.isEnabled();
            if (parentB != null && !parentB.isEnabled()) {
                enabled = false;
            }

            if (!enabled) {
                if (random.nextDouble() < 0.75) {
                    connection.setEnabled(false);
                }
            }

            child.addConnection(connection);
        }
        return child;
    }
}
