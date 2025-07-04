package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.mutation.*;

import java.util.HashMap;
import java.util.Random;

public class MutationService {
    private Random random = new Random();
    private final InnovationService innovationService;
    //    public List<Mutation> mutations = new ArrayList<>();
    private HashMap<Mutation, Double> mutations = new HashMap<>();

    public MutationService(NeatConfiguration configuration, InnovationService innovationService) {
        this.innovationService = innovationService;
        addMutation(new AddNeuronMutation(innovationService), configuration.mutateAddNeuronProbability);
        addMutation(new AddConnectionMutation(innovationService), configuration.mutateAddConnectionProbability);
        addMutation(new ToggleConnectionMutation(), configuration.mutateToggleConnectionProbability);
        addMutation(new WeightMutation(configuration.mutateRandomizeWeightsProbability, configuration.mutateWeightPerturbationPower), configuration.mutateWeightProbability);
    }

    private void addMutation(Mutation mutation, double probability) {
        mutations.put(mutation, probability);
    }

    /**
     * Apply a random mutation to the specified genome
     */
    public void mutateGenome(Genome genome) {
        for (Mutation mutation : mutations.keySet()) {
            double probability = mutations.get(mutation);

            if (random.nextDouble() < probability) {
                mutation.mutate(genome);
                return;
            }
        }
    }
}
