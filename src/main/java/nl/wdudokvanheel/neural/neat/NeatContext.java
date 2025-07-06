package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.MutationService;
import nl.wdudokvanheel.neural.neat.service.SpeciationService;

import java.util.ArrayList;
import java.util.List;

/**
 * The Neat Context contains all creatures, species and services to run the NEAT evolution logic. The context is completely
 * independent and self-contained, multiple contexts can be used to run different evolutions.
 */
public class NeatContext {
	public Creature blueprint;
    public CreatureFactory creatureFactory;
    public NeatConfiguration configuration;
    public InnovationService innovationService;
    public CrossoverService crossoverService;
    public MutationService mutationService;
    public SpeciationService speciationService;

    public int generation = 0;

    public List<Creature> creatures = new ArrayList<>();
    public List<Species> species = new ArrayList<>();

    public NeatContext(CreatureFactory creatureFactory) {
        this(creatureFactory, new NeatConfiguration());
    }

    public NeatContext(CreatureFactory creatureFactory, NeatConfiguration configuration) {
        this.creatureFactory = creatureFactory;
        this.configuration = configuration;
        innovationService = new InnovationService();
        crossoverService = new CrossoverService();
        mutationService = new MutationService(configuration, innovationService);
        speciationService = new SpeciationService(configuration);
    }

    public Creature getFittestCreature() {
        Creature fittest = null;

        for (Creature creature : creatures) {
            if (fittest == null || creature.getFitness() > fittest.getFitness()) {
                fittest = creature;
            }
        }

        return fittest;
    }
}
