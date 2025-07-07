package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.genome.Genome;

public interface CreatureInterface<Creature extends CreatureInterface<Creature>> {
    Genome getGenome();

    double getFitness();

    Species<Creature> getSpecies();

    void setSpecies(Species<Creature> species);

    void setFitness(double fitness);
}