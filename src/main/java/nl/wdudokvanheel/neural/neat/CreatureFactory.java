package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public interface CreatureFactory<C extends Creature> {
    /**
     * Create a new creature with the specified genome
     */
    C createNewCreature(Genome genome);
}
