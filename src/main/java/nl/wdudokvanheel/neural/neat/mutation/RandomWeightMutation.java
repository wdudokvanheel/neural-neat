package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;

public class RandomWeightMutation extends AbstractMutation {
    private static final double SIGMA = 0.5;
    private double mutateConnectionWeightProbability;

    public RandomWeightMutation(double mutateConnectionWeightProbability) {
        this.mutateConnectionWeightProbability = mutateConnectionWeightProbability;
    }

    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }

            if (random.nextDouble() < mutateConnectionWeightProbability) {
                double w = connection.getWeight() + getRandomGaussian(SIGMA);
                connection.setWeight(w);
            }
        }
    }
}
