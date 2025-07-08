package nl.wdudokvanheel.neat;

import nl.wdudokvanheel.neural.neat.*;
import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.InputNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.OutputNeuronGene;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.MutationService;
import nl.wdudokvanheel.neural.util.AbstractCreatureInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NeatEvolutionTest {
    private static class TestCreature extends AbstractCreatureInterface<TestCreature> {
        TestCreature(Genome g) { super(g); }
    }

    private static class DummyFactory implements CreatureFactory<TestCreature> {
        @Override public TestCreature createNewCreature(Genome g) { return new TestCreature(g); }
    }

    /** MutationService stub – does nothing for deterministic tests. */
    private static class NoOpMutation extends MutationService {
        NoOpMutation(NeatConfiguration cfg, InnovationService inv) { super(cfg, inv); }
        @Override public void mutateGenome(Genome g) { /* no-op */ }
    }

    private static Genome signedGenome(InnovationService inv, double weight) {
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in = b.addInputNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        ConnectionGene c = b.addConnection(in, out);
        c.setWeight(weight);
        return b.getGenome();
    }

    @Test
    @DisplayName("createContext wires all services and starts empty")
    void contextInitialisation() {
        NeatContext<TestCreature> ctx = NeatEvolution.createContext(new DummyFactory());

        assertNotNull(ctx.configuration);
        assertNotNull(ctx.innovationService);
        assertNotNull(ctx.mutationService);
        assertNotNull(ctx.crossoverService);
        assertNotNull(ctx.speciationService);
        assertEquals(0, ctx.generation);
        assertTrue(ctx.creatures.isEmpty());
        assertTrue(ctx.species.isEmpty());
        assertNull(ctx.blueprint);
    }

    @Test
    @DisplayName("generateInitialPopulation fills population and creates species")
    void initialPopulationProperties() {
        NeatContext<TestCreature> ctx = NeatEvolution.createContext(new DummyFactory());
        ctx.configuration.populationSize = 6;
        ctx.configuration.setInitialLinks = false;   // deterministic

        Genome tpl = signedGenome(ctx.innovationService, 0.0);
        TestCreature blueprint = new TestCreature(tpl);

        NeatEvolution.generateInitialPopulation(ctx, blueprint);

        assertEquals(6, ctx.creatures.size());
        assertFalse(ctx.species.isEmpty(), "creatures were not speciated");
        assertSame(blueprint, ctx.blueprint, "blueprint field not set correctly");
    }

    @Test
    @DisplayName("RandomWeightMutation alters at least one cloned genome")
    void mutationOccursOnClones() {
        NeatContext<TestCreature> ctx = NeatEvolution.createContext(new DummyFactory());
        ctx.configuration.populationSize = 4;          // one blueprint + three clones
        ctx.configuration.setInitialLinks = false;

        Genome tpl = signedGenome(ctx.innovationService, 0.0);
        TestCreature blueprint = new TestCreature(tpl);

        NeatEvolution.generateInitialPopulation(ctx, blueprint);

        // collect weights of the single connection in every genome
        Set<Double> weights = new HashSet<>();
        ctx.creatures.forEach(c ->
                weights.add(c.getGenome().getConnections().getFirst().getWeight()));

        assertTrue(weights.size() > 1,
                "all weights identical – RandomWeightMutation never fired");
    }

    @Test
    @DisplayName("nextGeneration advances generation and keeps population constant")
    void generationAdvancesAndPopulationStable() {
        NeatConfiguration cfg = new NeatConfiguration();
        cfg.populationSize = 8;
        cfg.reproduceWithoutCrossover = 1.0;   // asexual only for determinism
        cfg.setInitialLinks = false;

        NeatContext<TestCreature> ctx = new NeatContext(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutation(cfg, ctx.innovationService);

        TestCreature blueprint = new TestCreature(signedGenome(ctx.innovationService, 0.1));
        NeatEvolution.generateInitialPopulation(ctx, blueprint);

        int before = ctx.generation;
        NeatEvolution.nextGeneration(ctx);

        assertEquals(before + 1, ctx.generation);
        assertEquals(cfg.populationSize, ctx.creatures.size());
        assertFalse(ctx.species.isEmpty());
    }

    @Test
    @DisplayName("bottomElimination removes the specific low-fitness individuals")
    void bottomEliminationOccurs() {
        NeatConfiguration cfg = new NeatConfiguration();
        cfg.populationSize       = 5;
        cfg.bottomElimination    = 0.6;     // drop 3 of 5
        cfg.reproduceWithoutCrossover = 1.0;
        cfg.setInitialLinks      = false;

        NeatContext<TestCreature> ctx = new NeatContext(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutation(cfg, ctx.innovationService);

        // initial population (generates blueprint too)
        TestCreature blueprint = new TestCreature(signedGenome(ctx.innovationService, 0.3));
        NeatEvolution.generateInitialPopulation(ctx, blueprint);

        // assign deterministic fitness 0..4
        AtomicInteger f = new AtomicInteger();
        ctx.creatures.forEach(c -> c.setFitness(f.getAndIncrement()));

        // remember the three creatures with fitness 0,1,2 that should be culled
        Set<CreatureInterface> expectedToDie = ctx.creatures.stream()
                .filter(c -> c.getFitness() <= 2)
                .collect(Collectors.toSet());

        NeatEvolution.nextGeneration(ctx);

        expectedToDie.forEach(c ->
                assertFalse(ctx.creatures.contains(c), "eliminated creature is still present"));
    }
}
