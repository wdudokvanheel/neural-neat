package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.GenomeComparison;
import nl.wdudokvanheel.neural.neat.model.Creature;
import nl.wdudokvanheel.neural.neat.model.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.model.NeatContext;
import nl.wdudokvanheel.neural.neat.model.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SpeciationService {
    private Logger logger = LoggerFactory.getLogger(SpeciationService.class);

    private Random random = new Random();
    private NeatConfiguration configuration;

    public SpeciationService(NeatConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<Species> speciate(List<Creature> creatures, List<Species> species) {
        for (Creature creature : creatures) {
            addCreatureToSpecies(species, creature);
        }
        eliminateEmptySpecies(species);
        logger.trace("Speciated {} creatures into {} species", creatures.size(), species.size());
        return species;
    }

    public void eliminateStagnantSpecies(List<Species> species) {
        for (Iterator<Species> iterator = species.iterator(); iterator.hasNext(); ) {
            Species test = iterator.next();

            if (test.getFitness() > test.lastFitness) {
                test.lastImprovement = 0;
                test.lastFitness = test.getFitness();
            } else {
                test.lastImprovement++;
            }
            if (test.lastImprovement >= 15 && species.size() > 1) {
                iterator.remove();
                logger.trace("Killing species {}", test);
            }
        }
    }

    public void eliminateEmptySpecies(List<Species> species) {
        for (Iterator<Species> iterator = species.iterator(); iterator.hasNext(); ) {
            Species test = iterator.next();
            if (test.size() == 0) {
                logger.trace("Removed empty species {}", test);
                iterator.remove();
            }
        }
    }

    public List<Species> createNewGenerationSpecies(NeatContext context) {
        ArrayList<Species> newSpecies = new ArrayList<>();

        for (Species iter : context.species) {
            //Get a random creature from the current species as the representative of the new species
            Creature representative = iter.getCreatures().get(random.nextInt(iter.size()));
            Species replacement = new Species(iter.id, representative);
            replacement.lastImprovement = iter.lastImprovement;
            replacement.lastFitness = iter.lastFitness;
            newSpecies.add(replacement);
        }

        return newSpecies;
    }

    public List<Creature> getChampions(NeatContext context) {
        ArrayList<Creature> creatures = new ArrayList<>();
        for (Species iter : context.species) {
            if (iter.size() >= context.configuration.minimumSpeciesSizeForChampionCopy) {
                Creature champion = context.creatureFactory.createNewCreature(iter.getChampion().getGenome().clone());
                creatures.add(champion);
			}
        }
        return creatures;
    }

    public void sortCreatures(List<Species> species) {
        species.forEach(this::sortCreaturesByScore);
    }

    public void sortCreaturesByScore(Species species) {
        species.getCreatures().sort(Comparator.comparingDouble(Creature::getFitness).reversed());
    }

    public List<Species> sortSpeciesByScore(List<Species> species) {
        return species.stream().sorted(Comparator.comparingDouble(Species::getFitness).reversed()).collect(Collectors.toList());
    }

    public void eliminateLeastFitCreatures(List<Species> species) {
        for (Species iter : species) {
            int toEliminate = (int) Math.floor(iter.getCreatures().size() * configuration.bottomElimination);
            logger.trace("Eliminating {} creatures from species {}", toEliminate, iter);
            while (toEliminate > 0) {
                iter.getCreatures().remove(iter.getCreatures().size() - 1);
                toEliminate--;
            }
            logger.trace("Eliminated creatures from species {}", iter);
        }
    }

    private Species addCreatureToSpecies(List<Species> species, Creature creature) {
        //Try to add the creature to any existing species
        for (Species existingSpecies : species) {
            //Compare the creature's genome with the representative of the species
            double distance = new GenomeComparison(existingSpecies.getRepresentative().getGenome(), creature.getGenome()).getDistance();
            if (distance < configuration.speciesThreshold) {
                logger.trace("Adding creature to species");
                existingSpecies.addCreature(creature);
                creature.setSpecies(existingSpecies);
                return existingSpecies;
            }
        }

        //Creature did not match with any existing species, create a new one
        Species newSpecies = new Species(creature);
        species.add(newSpecies);
        creature.setSpecies(newSpecies);
        logger.trace("Creating new species for creature");

        return newSpecies;
    }
}
