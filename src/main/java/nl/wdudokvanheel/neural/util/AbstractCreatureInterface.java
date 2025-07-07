package nl.wdudokvanheel.neural.util;

import nl.wdudokvanheel.neural.neat.CreatureInterface;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.Genome;

/**
 * Small helper class to implement the basic requirements for the Creature interface
 */
public class AbstractCreatureInterface<Creature extends CreatureInterface<Creature>> implements CreatureInterface<Creature> {
    private final Genome genome;
    private Species<Creature> species;
    private double score;

    public AbstractCreatureInterface(Genome genome) {
        this.genome = genome;
    }

    public AbstractCreatureInterface(Genome genome, Species<Creature> species) {
        this.genome = genome;
        this.species = species;
    }

    @Override
    public Genome getGenome() {
        return genome;
    }

    @Override
    public Species<Creature> getSpecies() {
        return species;
    }

    public void setSpecies(Species<Creature> species) {
        this.species = species;
    }

    @Override
    public double getFitness() {
        return score;
    }

    @Override
    public void setFitness(double fitness) {
        this.score = fitness;
    }
}
