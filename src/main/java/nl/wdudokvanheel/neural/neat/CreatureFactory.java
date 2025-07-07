package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public interface CreatureFactory<Creature extends CreatureInterface<Creature>> {
    /**
     * Create a new creature with the specified genome
     */
    Creature createNewCreature(Genome genome);
}
