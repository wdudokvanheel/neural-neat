package nl.wdudokvanheel.neural;

import nl.wdudokvanheel.neural.neat.model.Creature;
import nl.wdudokvanheel.neural.neat.model.Genome;

public interface CreatureFactory<T extends Creature> {
    /**
     * Create a new creature with the specified genome
     */
    T createNewCreature(Genome genome);
}
