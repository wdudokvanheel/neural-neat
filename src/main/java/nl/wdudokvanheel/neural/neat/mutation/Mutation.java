package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public interface Mutation {
    /**
     * Perform the mutation on a genome
     */
    void mutate(Genome genome);
}
