package nl.wdudokvanheel.neural.util;

import nl.wdudokvanheel.neural.neat.Creature;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.Genome;

/**
 * Small helper class to implement the basic requirements for the Creature interface
 */
public class AbstractCreature implements Creature {
    private final Genome genome;
    private Species species;
    private double score;

    public AbstractCreature(Genome genome) {
        this.genome = genome;
    }

    public AbstractCreature(Genome genome, Species species) {
        this.genome = genome;
        this.species = species;
    }

    @Override
    public Genome getGenome() {
        return genome;
    }

    @Override
    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
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
