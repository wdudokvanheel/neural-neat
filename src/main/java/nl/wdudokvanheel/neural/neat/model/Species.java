package nl.wdudokvanheel.neural.neat.model;

import java.util.ArrayList;
import java.util.List;

public class Species {
    //TEMP
    private static int counter = 0;
    public int id;
    private List<Creature> creatures = new ArrayList<>();
    private Creature representative;

    public double lastFitness = 0;
    public int lastImprovement = 0;

    public Species(Creature representative) {
        id = ++counter;
        this.representative = representative;
        addCreature(representative);
    }

    public Species(int id, Creature representative) {
        this.id = id;
        this.representative = representative;
        addCreature(representative);
    }

    public void addCreature(Creature creature) {
        if (creatures.contains(creature)) {
            throw new IllegalArgumentException("Creature " + creature + " is already present in Species #" + id);
        }

        creatures.add(creature);
    }

    public List<Creature> getCreatures() {
        return creatures;
    }

    public Creature getRepresentative() {
        return representative;
    }

    /**
     * Get the fittest creature of this species
     */
    public Creature getChampion() {
        Creature fittest = null;
        for (Creature creature : creatures) {
            if (fittest == null || creature.getFitness() > fittest.getFitness()) {
                fittest = creature;
            }
        }

        return fittest;
    }

    /**
     * Get the average fitness of this species
     *
     * @return
     */
    public double getFitness() {
        if (creatures.size() == 0)
            return 0;

        double total = 0;
        for (Creature creature : creatures) {
            total += creature.getFitness();
        }

        if (total == 0) {
            return 0;
        }

        return total / creatures.size();
    }

    public int size() {
        return creatures.size();
    }

    @Override
    public String toString() {
        return "Species #" + id + " fittest: " + (getChampion() != null ? "" + getChampion().getFitness() : "none");
    }
}
