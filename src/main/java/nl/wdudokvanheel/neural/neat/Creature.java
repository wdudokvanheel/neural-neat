package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public interface Creature {
    Genome getGenome();

    double getFitness();

    Species getSpecies();

    void setSpecies(Species species);

    void setFitness(double fitness);
}
