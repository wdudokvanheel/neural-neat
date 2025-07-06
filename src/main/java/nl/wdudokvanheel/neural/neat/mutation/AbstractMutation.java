package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;

import java.util.List;
import java.util.Random;

/**
 * Abstract mutation with some common helper methods
 */
public abstract class AbstractMutation implements Mutation {
    protected Random random = new Random();

    protected ConnectionGene getRandomConnection(Genome genome) {
        if (genome.getConnections().size() == 0) {
            return null;
        }

        return getRandomElement(genome.getConnections());
    }

    public double getRandomDouble(double min, double max) {
        return random.nextDouble(min, max);
    }

    /**
     * Get a random element from a list
     *
     * @return Null if the list is empty
     */
    public <T> T getRandomElement(List<T> list) {
        if (list.size() == 0) {
            return null;
        }

        return list.get(random.nextInt(list.size()));
    }

    public double getRandomGaussian(double scale) {
        return scale * random.nextGaussian();
    }
}
