package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;

public class ShiftWeightMutation extends AbstractMutation {
    private double perturbationPower;

    public ShiftWeightMutation(double perturbationPower) {
        this.perturbationPower = perturbationPower;
    }

    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }

            double perturbation = getRandomGaussian(2);
            perturbation *= perturbationPower;
            connection.setWeight(connection.getWeight() + perturbation);
        }
    }
}
