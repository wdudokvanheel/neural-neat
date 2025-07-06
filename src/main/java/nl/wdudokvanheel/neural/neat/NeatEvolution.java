package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.CreatureFactory;
import nl.wdudokvanheel.neural.neat.model.*;
import nl.wdudokvanheel.neural.neat.mutation.RandomWeightMutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeatEvolution {
    private static Logger logger = LoggerFactory.getLogger(NeatEvolution.class);
    private static Random random = new Random();

    /**
     * Create a new NEAT context with default values. The initial creature will be cloned and modified to create the population
     *
     * @param factory The creature factory to generate new creatures
     * @return A neat context
     */
    public static NeatContext createContext(CreatureFactory factory) {
        return new NeatContext(factory);
    }

    public static NeatContext createContext(CreatureFactory factory, NeatConfiguration configuration) {
        return new NeatContext(factory, configuration);
    }

    public static void generateInitialPopulation(NeatContext context, Creature blueprint) {
        context.blueprint = blueprint;
        logger.trace("Generating initial population of {}", context.configuration.populationSize);
        int count = 1;

        //Clone & mutate the blueprint creature to fill the remaining population
        while (context.creatures.size() < context.configuration.populationSize) {
            Genome clone = blueprint.getGenome().clone();

            //Don't mutate the first creature
            if (count > 1) {
                new RandomWeightMutation().mutate(clone);
            }

            if (context.configuration.setInitialLinks) {
                initialConnectionState(clone, context.configuration.initialLinkActiveProbability);
            }

            Creature creature = context.creatureFactory.createNewCreature(clone);
            context.creatures.add(creature);
            count++;
        }

        //Speciate initial pop
        context.species = context.speciationService.speciate(context.creatures, new ArrayList<Species>());
    }

    private static void initialConnectionState(Genome genome, double linkProbability) {
        for (ConnectionGene connection : genome.getConnections()) {
            connection.setEnabled(random.nextDouble() < linkProbability);
        }
    }

    public static void nextGeneration(NeatContext context) {
        context.generation++;
        logger.trace("");
        logger.trace("===== Starting generation {} =====", context.generation);
        logger.trace("This generation has {} creatures & {} species", context.creatures.size(), context.species.size());

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
        List<Species> newSpecies = context.speciationService.createNewGenerationSpecies(context);

        int newEmptyCreatures = (int) (context.configuration.populationSize * context.configuration.newCreaturesPerGeneration);
        logger.trace("Creating {} new empty creatures", newEmptyCreatures);
        List<Creature> newCreatures = createNewCreatures(context, newEmptyCreatures);

        if (context.configuration.copyChampionsAllSpecies) {
            //Create a new list of creatures, starting with clones of the (non-mutated) champions of the current generation
            List<Creature> champions = context.speciationService.getChampions(context);
            logger.trace("Adding {} champions from the previous generation", champions.size());
            newCreatures.addAll(champions);
        }

        //Crossover creatures
        logger.trace("Creating {} offspring", context.configuration.populationSize - newCreatures.size());
        List<Creature> offspring = context.crossoverService.createOffspring(context, context.configuration.populationSize - newCreatures.size());
        newCreatures.addAll(offspring);

        //Clear last generation creatures and add new ones
        context.creatures.clear();
        context.creatures.addAll(newCreatures);

        context.species.clear();

        //Speciate newly created creatures
        context.species.addAll(context.speciationService.speciate(newCreatures, newSpecies));

        //TODO move to Speciationservice
        if (context.configuration.adjustSpeciesThreshold) {
            if (context.species.size() < context.configuration.targetSpecies) {
                context.configuration.speciesThreshold *= 0.9;
            } else if (context.species.size() > context.configuration.targetSpecies) {
                context.configuration.speciesThreshold *= 1.1;
            }

            context.configuration.speciesThreshold = Math.max(
                    context.configuration.minSpeciesThreshold,
                    Math.min(context.configuration.speciesThreshold, context.configuration.maxSpeciesThreshold)
            );
        }
    }

    private static ArrayList<Creature> createNewCreatures(NeatContext context, int creatures) {
        ArrayList<Creature> result = new ArrayList<>();
        int count = 0;
        while (count < creatures) {
            count++;
            Genome clone = context.blueprint.getGenome().clone();

            new RandomWeightMutation().mutate(clone);

            if (context.configuration.setInitialLinks) {
                initialConnectionState(clone, context.configuration.initialLinkActiveProbability);
            }

            Creature creature = context.creatureFactory.createNewCreature(clone);
            result.add(creature);
        }
        return result;
    }
}
