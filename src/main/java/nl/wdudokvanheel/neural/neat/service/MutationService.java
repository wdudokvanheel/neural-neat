package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.mutation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MutationService {
    private Random random = new Random();
    private NeatConfiguration configuration;
    private Map<Mutation, Double> mutations = new LinkedHashMap<>();

    public MutationService(NeatConfiguration configuration, InnovationService innovationService) {
        this.configuration = configuration;

        addMutation(new AddNeuronMutation(innovationService), configuration.mutateAddNeuronProbability);
        addMutation(new AddConnectionMutation(innovationService), configuration.mutateAddConnectionProbability);
        addMutation(new ToggleConnectionMutation(), configuration.mutateToggleConnectionProbability);
        addMutation(
                new WeightMutation(
                        configuration.mutateRandomizeWeightsProbability,
                        configuration.mutateWeightPerturbationPower,
                        configuration.mutateConnectionWeightProbability
                ),
                configuration.mutateWeightProbability
        );
    }

    private void addMutation(Mutation mutation, double probability) {
        mutations.put(mutation, probability);
    }

    public void mutateGenome(Genome genome) {
        if (configuration.multipleMutationsPerGenome) {
            for (var entry : mutations.entrySet()) {
                if (random.nextDouble() < entry.getValue()) {
                    entry.getKey().mutate(genome);
                }
            }
        } else {
            double roll = random.nextDouble();
            double cumulative = 0;
            for (var entry : mutations.entrySet()) {
                cumulative += entry.getValue();
                if (roll < cumulative) {
                    entry.getKey().mutate(genome);
                    break;
                }
            }
        }
    }
}
