package nl.wdudokvanheel.neural;

import nl.wdudokvanheel.neural.neat.model.Creature;
import nl.wdudokvanheel.neural.neat.model.Genome;

public interface CreatureFactory<C extends Creature> {
    /**
     * Create a new creature with the specified genome
     */
    C createNewCreature(Genome genome);
}
