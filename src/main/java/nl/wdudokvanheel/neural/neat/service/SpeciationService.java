package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.CreatureInterface;
import nl.wdudokvanheel.neural.neat.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.NeatContext;
import nl.wdudokvanheel.neural.neat.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SpeciationService<Creature extends CreatureInterface<Creature>> {
    private Logger logger = LoggerFactory.getLogger(SpeciationService.class);

    private Random random = new Random();
    private NeatConfiguration configuration;

    public SpeciationService(NeatConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<Species<Creature>> speciate(List<Creature> creatures, List<Species<Creature>> species) {
        for (Creature creature : creatures) {
            addCreatureToSpecies(species, creature);
        }
        eliminateEmptySpecies(species);
        logger.trace("Speciated {} creatures into {} species", creatures.size(), species.size());
        return species;
    }

    public void eliminateStagnantSpecies(List<Species<Creature>> species) {
        for (Iterator<Species<Creature>> iterator = species.iterator(); iterator.hasNext(); ) {
            Species<Creature> test = iterator.next();

            if (test.getFitness() > test.lastFitness) {
                test.lastImprovement = 0;
                test.lastFitness = test.getFitness();
            } else {
                test.lastImprovement++;
            }
            if (test.lastImprovement >= 15 && species.size() > 1) {
                iterator.remove();
                logger.trace("Killing species {}", test);
            }
        }
    }

    public void eliminateEmptySpecies(List<Species<Creature>> species) {
        for (Iterator<Species<Creature>> iterator = species.iterator(); iterator.hasNext(); ) {
            Species<Creature> test = iterator.next();
            if (test.size() == 0) {
                logger.trace("Removed empty species {}", test);
                iterator.remove();
            }
        }
    }

    public List<Species<Creature>> createNewGenerationSpecies(NeatContext<Creature> context) {
        ArrayList<Species<Creature>> newSpecies = new ArrayList<>();

        for (Species<Creature> iter : context.species) {
            //Get the champion from the current species as the representative of the new species
            Creature representative = iter.getChampion();
            Species<Creature> replacement = new Species<>(iter.id, representative);
            replacement.addCreature(representative);
            replacement.lastImprovement = iter.lastImprovement;
            replacement.lastFitness = iter.lastFitness;
            newSpecies.add(replacement);
        }

        return newSpecies;
    }

    public List<Creature> getChampions(NeatContext<Creature> context) {
        ArrayList<Creature> creatures = new ArrayList<>();
        for (Species<Creature> iter : context.species) {
            if (iter.size() >= context.configuration.minimumSpeciesSizeForChampionCopy) {
                Creature champion = context.creatureFactory.createNewCreature(iter.getChampion().getGenome().clone());
                creatures.add(champion);
            }
        }
        return creatures;
    }

    public void sortCreatures(List<Species<Creature>> species) {
        species.forEach(this::sortCreaturesByScore);
    }

    public void sortCreaturesByScore(Species<Creature> species) {
        species.getCreatures().sort(Comparator.comparingDouble(Creature::getFitness).reversed());
    }

    public List<Species<Creature>> sortSpeciesByScore(List<Species<Creature>> species) {
        return species.stream().sorted(Comparator.comparingDouble(Species<Creature>::getFitness).reversed()).collect(Collectors.toList());
    }

    public void eliminateLeastFitCreatures(List<Species<Creature>> species) {
        for (Species<Creature> iter : species) {
            int toEliminate = (int) Math.floor(iter.getCreatures().size() * configuration.bottomElimination);
            logger.trace("Eliminating {} creatures from species {}", toEliminate, iter);
            while (toEliminate > 0) {
                iter.getCreatures().removeLast();
                toEliminate--;
            }
            logger.trace("Eliminated creatures from species {}", iter);
        }
    }

    private void addCreatureToSpecies(List<Species<Creature>> species, Creature creature) {
        // Shuffle species so the first ones don't automatically fill up
        Collections.shuffle(species, random);
        //Try to add the creature to any existing species
        for (Species<Creature> existingSpecies : species) {
            //Compare the creature's genome with the representative of the species
            double distance = new GenomeComparison(existingSpecies.getRepresentative().getGenome(), creature.getGenome()).getDistance();
            if (distance < configuration.speciesThreshold) {
                logger.trace("Adding creature to species");
                existingSpecies.addCreature(creature);
                creature.setSpecies(existingSpecies);
                return;
            }
        }

        //Creature did not match with any existing species, create a new one
        Species<Creature> newSpecies = new Species<>(creature);
        species.add(newSpecies);
        creature.setSpecies(newSpecies);
        logger.trace("Creating new species for creature");

    }

    public void adjustThreshold(List<Species<Creature>> species) {
        if (species.size() < configuration.targetSpecies) {
            configuration.speciesThreshold *= 0.9;
        } else if (species.size() > configuration.targetSpecies) {
            configuration.speciesThreshold *= 1.1;
        }

        configuration.speciesThreshold = Math.max(
                configuration.minSpeciesThreshold,
                Math.min(configuration.speciesThreshold, configuration.maxSpeciesThreshold)
        );
    }
}
