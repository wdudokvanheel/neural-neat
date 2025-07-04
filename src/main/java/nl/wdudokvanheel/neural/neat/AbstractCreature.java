package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.model.Creature;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.Species;

public class AbstractCreature implements Creature {
    private Genome genome;
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
