package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;

public class ShiftWeightMutation extends AbstractMutation {
    private final double perturbationPower;
    private final double mutateConnectionWeightProbability;

    public ShiftWeightMutation(double perturbationPower, double mutateConnectionWeightProbability) {
        this.perturbationPower = perturbationPower;
        this.mutateConnectionWeightProbability = mutateConnectionWeightProbability;
    }

    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }

            if (random.nextDouble() < mutateConnectionWeightProbability) {
                double p = getRandomGaussian(2) * perturbationPower;
                connection.setWeight(connection.getWeight() + p);
            }
        }
    }
}