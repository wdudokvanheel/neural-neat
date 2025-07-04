package nl.wdudokvanheel.neural.neat.model;

public interface Creature {
    Genome getGenome();

    double getFitness();

    Species getSpecies();

    void setSpecies(Species species);

    void setFitness(double fitness);
}
