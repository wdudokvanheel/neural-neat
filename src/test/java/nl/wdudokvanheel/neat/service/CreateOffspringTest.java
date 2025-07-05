package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.CreatureFactory;
import nl.wdudokvanheel.neural.neat.AbstractCreature;
import nl.wdudokvanheel.neural.neat.model.*;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.MutationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression-suite for CrossoverService.createOffspring(...)
 */
class CreateOffspringTest {
    /**
     * Minimal creature that carries a genome and writable fitness.
     */
    private static class DummyCreature extends AbstractCreature {
        DummyCreature(Genome g, double fitness) {
            super(g);
            setFitness(fitness);
        }
    }

    /**
     * Factory that always returns a new DummyCreature.
     */
    private static class DummyFactory implements CreatureFactory<DummyCreature> {
        @Override
        public DummyCreature createNewCreature(Genome genome) {
            return new DummyCreature(genome, 0);
        }
    }

    /**
     * MutationService stub that does nothing, for deterministic genomes.
     */
    private static class NoOpMutationService extends MutationService {
        NoOpMutationService(NeatConfiguration cfg, InnovationService inv) {
            super(cfg, inv);
        }

        @Override
        public void mutateGenome(Genome g) { /* no-op */ }
    }

    /**
     * Build a genome that contains exactly one distinguishing input neuron.
     */
    private static Genome genomeWithSignature(int neuronId) {
        Genome g = new Genome();
        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, neuronId, 0));
        return g;
    }

    /**
     * Replace CrossoverService.random with a seedable instance for reproducibility.
     */
    private static CrossoverService serviceWithSeed(long seed) throws Exception {
        CrossoverService xsv = new CrossoverService();
        Field rnd = CrossoverService.class.getDeclaredField("random");
        rnd.setAccessible(true);
        rnd.set(xsv, new Random(seed));
        return xsv;
    }

    /**
     * Count offspring carrying a given signature neuron.
     */
    private static long countWithSignature(List<Creature> list, int neuronId) {
        return list.stream().filter(c -> c.getGenome().getNeuronById(neuronId) != null).count();
    }

    @Test
    @DisplayName("Returns empty list when population ≤ 0")
    void emptyPopulationEarlyExit() throws Exception {
        NeatContext ctx = new NeatContext(new DummyFactory());
        CrossoverService xsv = serviceWithSeed(0);
        assertTrue(xsv.createOffspring(ctx, 0).isEmpty());
        assertTrue(xsv.createOffspring(ctx, -5).isEmpty());
    }

    @Test
    @DisplayName("With all-zero fitness, offspring come exclusively from first species")
    void zeroFitnessAllotment() throws Exception {
        // signatures 101 & 202 distinguish the genomes
        Species first = new Species(new DummyCreature(genomeWithSignature(101), 0));
        Species second = new Species(new DummyCreature(genomeWithSignature(202), 0));

        NeatConfiguration cfg = new NeatConfiguration();
        cfg.reproduceWithoutCrossover = 1.0;     // purely asexual
        cfg.interspeciesCrossover = 0.0;

        NeatContext ctx = new NeatContext(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutationService(cfg, ctx.innovationService);
        ctx.species.addAll(List.of(first, second));

        CrossoverService xsv = serviceWithSeed(1);
        List<Creature> off = xsv.createOffspring(ctx, 6);

        assertEquals(6, off.size(), "should create the requested number of offspring");
        assertEquals(6, countWithSignature(off, 101), "all offspring should inherit from first species");
        assertEquals(0, countWithSignature(off, 202), "no offspring should come from second species");
    }

    @Test
    @DisplayName("Offspring quota follows relative fitness proportions")
    void weightedQuotaDistribution() throws Exception {
        Species strong = new Species(new DummyCreature(genomeWithSignature(111), 30)); // fitness 30
        Species weak = new Species(new DummyCreature(genomeWithSignature(222), 10)); // fitness 10

        NeatConfiguration cfg = new NeatConfiguration();
        cfg.reproduceWithoutCrossover = 1.0;
        cfg.interspeciesCrossover = 0.0;

        NeatContext ctx = new NeatContext(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutationService(cfg, ctx.innovationService);
        ctx.species.addAll(List.of(strong, weak));

        CrossoverService xsv = serviceWithSeed(42);
        int population = 8;                       // total offspring requested
        List<Creature> off = xsv.createOffspring(ctx, population);

        assertEquals(population, off.size(), "offspring count mismatch");

        long strongKids = countWithSignature(off, 111);
        long weakKids = countWithSignature(off, 222);

        assertEquals(6, strongKids, "strong species should receive 6 offspring (¾ of pop)");
        assertEquals(2, weakKids, "weak species should receive 2 offspring (¼ of pop)");
        assertEquals(population, strongKids + weakKids);
    }
}