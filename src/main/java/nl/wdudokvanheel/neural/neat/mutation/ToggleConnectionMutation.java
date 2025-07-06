package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;

public class ToggleConnectionMutation extends AbstractMutation {
    private static final double SIGMA = 1.0;

    @Override
    public void mutate(Genome genome) {
        if (genome.getConnections().isEmpty()) {
            return;
        }
        ConnectionGene connection = getRandomConnection(genome);
        connection.toggleEnabled();
        double w = getRandomGaussian(SIGMA);
        connection.setWeight(w);
    }
}
