package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public class WeightMutation extends AbstractMutation {
    private double randomizeWeightsProbability;
    private RandomWeightMutation randomWeightMutation;
    private ShiftWeightMutation shiftWeightMutation;

    public WeightMutation(double randomizeWeightsProbability, double mutateWeightPerturbationPower, double mutateConnectionWeightProbability) {
        this.randomizeWeightsProbability = randomizeWeightsProbability;
        this.randomWeightMutation = new RandomWeightMutation(mutateConnectionWeightProbability);
        this.shiftWeightMutation = new ShiftWeightMutation(mutateWeightPerturbationPower, mutateConnectionWeightProbability);
    }

    @Override
    public void mutate(Genome genome) {
        if (random.nextDouble() < randomizeWeightsProbability) {
            randomWeightMutation.mutate(genome);
        } else {
            shiftWeightMutation.mutate(genome);
        }
    }
}
