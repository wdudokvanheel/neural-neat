package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;

public class ShiftWeightMutation extends AbstractMutation {
    private final double perturbationPower;

    public ShiftWeightMutation(double perturbationPower) {
        this.perturbationPower = perturbationPower;
    }

    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }
            double p = getRandomGaussian(2) * perturbationPower;
            connection.setWeight(connection.getWeight() + p);
        }
    }
}