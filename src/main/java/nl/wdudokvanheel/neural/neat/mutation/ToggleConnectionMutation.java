package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;

public class ToggleConnectionMutation extends AbstractMutation {
    @Override
    public void mutate(Genome genome) {
        if (genome.getConnections().size() == 0) {
            return;
        }
        ConnectionGene connection = getRandomConnection(genome);
        connection.toggleEnabled();
        connection.setWeight(getRandomGaussian(8));
    }
}
