package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.mutation.RandomWeightMutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeatEvolution {
    private static final Logger logger = LoggerFactory.getLogger(NeatEvolution.class);
    private static final Random random = new Random();

    /**
     * Create a new NEAT context with default values.
     *
     * @param factory The creature factory to generate new creatures
     * @return A neat context
     */
    public static <Creature extends CreatureInterface<Creature>> NeatContext<Creature> createContext(CreatureFactory<Creature> factory) {
        return new NeatContext<>(factory);
    }

    public static <Creature extends CreatureInterface<Creature>> NeatContext<Creature> createContext(CreatureFactory<Creature> factory, NeatConfiguration configuration) {
        return new NeatContext<>(factory, configuration);
    }

    public static <Creature extends CreatureInterface<Creature>> void generateInitialPopulation(NeatContext<Creature> context, Creature blueprint) {
        generateInitialPopulation(context, blueprint, null);
    }

    public static <Creature extends CreatureInterface<Creature>> void generateInitialPopulation(NeatContext<Creature> context, Creature blueprint, Creature ultraChampion) {
        context.blueprint = blueprint;
        logger.trace("Generating initial population of {}", context.configuration.populationSize);

        if (ultraChampion != null) {
            context.creatures.add(ultraChampion);
            logger.trace("Adding {} Ultra Champions", context.configuration.ultraChampionClones);
            for (int i = 1; i < context.configuration.ultraChampionClones; i++) {
                Genome clone = ultraChampion.getGenome().clone();
                context.mutationService.mutateGenome(clone);
                Creature creature = context.creatureFactory.createNewCreature(clone);

                context.creatures.add(creature);
            }
        }

        int count = 1;
        //Clone & mutate the blueprint creature to fill the remaining population
        while (context.creatures.size() < context.configuration.populationSize) {
            Genome clone = blueprint.getGenome().clone();

            //Don't mutate the first creature
            if (count > 1) {
                if (context.configuration.setInitialLinks) {
                    initialConnectionState(clone, context.configuration.initialLinkActiveProbability, context.configuration.initialLinkWeight);
                }
            }

            Creature creature = context.creatureFactory.createNewCreature(clone);
            context.creatures.add(creature);
            count++;
        }

        //Speciate initial pop
        context.species = context.speciationService.speciate(context.creatures, new ArrayList<>());
    }

    private static void initialConnectionState(Genome genome, double linkProbability, double linkWeight) {
        for (ConnectionGene connection : genome.getConnections()) {
            connection.setEnabled(random.nextDouble() < linkProbability);

            if (connection.isEnabled()) {
                connection.setWeight(random.nextDouble() * (2 * linkWeight) - linkWeight);
            }
        }
    }

    public static <Creature extends CreatureInterface<Creature>> void nextGeneration
            (NeatContext<Creature> context) {
        context.generation++;
        logger.trace("");
        logger.trace("===== Starting generation {} =====", context.generation);
        logger.trace("This generation has {} creatures & {} species", context.creatures.size(), context.species.size());

        if (context.configuration.adjustSpeciesThreshold) {
            context.speciationService.adjustThreshold(context.species);
        }

        //Sort the creatures by fitness for each species
        context.speciationService.sortCreatures(context.species);

        //Eliminate stagnant species
        if (context.configuration.eliminateStagnantSpecies) {
            context.speciationService.eliminateStagnantSpecies(context.species);
        }

        //Eliminate the least fit creatures of each species
        logger.trace("Eliminating least fit creatures");
        context.speciationService.eliminateLeastFitCreatures(context.species);

        //Create new species with random representatives and a clone of the best performing creature
        List<Species<Creature>> newSpecies = context.speciationService.createNewGenerationSpecies(context);

        int newEmptyCreatures = (int) (context.configuration.populationSize * context.configuration.newCreaturesPerGeneration);
        logger.trace("Creating {} new empty creatures", newEmptyCreatures);
        List<Creature> newCreatures = createNewCreatures(context, newEmptyCreatures);

        if (context.configuration.copyChampionsAllSpecies) {
            //Create a new list of creatures, starting with clones of the (non-mutated) champions of the current generation
            List<Creature> champions = context.speciationService.getChampions(context);
            logger.trace("Adding {} champions from the previous generation", champions.size());
            newCreatures.addAll(champions);
            if (!newCreatures.contains(context.getFittestCreature())) {
                newCreatures.add(context.getFittestCreature());
            }
        }

        //Crossover creatures
        logger.trace("Creating {} offspring", context.configuration.populationSize - newCreatures.size());
        List<Creature> offspring = context.crossoverService.createOffspring(context, context.configuration.populationSize - newCreatures.size());
        newCreatures.addAll(offspring);

        //Clear last generation creatures and add new ones
        context.creatures = newCreatures;

        //Speciate newly created creatures
        context.species = context.speciationService.speciate(newCreatures, newSpecies);
    }

    private static <Creature extends CreatureInterface<Creature>> ArrayList<Creature> createNewCreatures
            (NeatContext<Creature> context, int creatures) {
        ArrayList<Creature> result = new ArrayList<>();
        int count = 0;
        while (count < creatures) {
            count++;
            Genome clone = context.blueprint.getGenome().clone();

            // Randomize all connection weights
            new RandomWeightMutation(1.0).mutate(clone);

            if (context.configuration.setInitialLinks) {
                initialConnectionState(clone, context.configuration.initialLinkActiveProbability, context.configuration.initialLinkWeight);
            }

            Creature creature = context.creatureFactory.createNewCreature(clone);
            result.add(creature);
        }
        return result;
    }
}
