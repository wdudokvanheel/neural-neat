package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;

public class RandomWeightMutation extends AbstractMutation {
    private static final double SIGMA = 0.5;

    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }

            double w = connection.getWeight() + getRandomGaussian(SIGMA);
            connection.setWeight(w);
        }
    }
}
