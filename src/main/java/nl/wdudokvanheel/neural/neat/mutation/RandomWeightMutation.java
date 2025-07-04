package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;

public class RandomWeightMutation extends AbstractMutation {
    @Override
    public void mutate(Genome genome) {
        for (ConnectionGene connection : genome.getConnections()) {
            if (!connection.isEnabled()) {
                continue;
            }

            connection.setWeight(getRandomGaussian(2));
        }
    }
}
