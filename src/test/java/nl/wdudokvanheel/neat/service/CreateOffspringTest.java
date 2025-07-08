package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.CreatureFactory;
import nl.wdudokvanheel.neural.neat.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.NeatContext;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.InputNeuronGene;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.MutationService;
import nl.wdudokvanheel.neural.util.AbstractCreatureInterface;
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
    private static class TestCreature extends AbstractCreatureInterface<TestCreature> {
        TestCreature(Genome g, double fitness) {
            super(g);
            setFitness(fitness);
        }
    }

    /**
     * Factory that always returns a new DummyCreature.
     */
    private static class DummyFactory implements CreatureFactory<TestCreature> {
        @Override
        public TestCreature createNewCreature(Genome genome) {
            return new TestCreature(genome, 0);
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
    private static class SignatureGenome {
        final Genome genome;
        final int innovationId;

        SignatureGenome(Genome genome, int innovationId) {
            this.genome = genome;
            this.innovationId = innovationId;
        }
    }

    private static SignatureGenome genomeWithSignature(int index) {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in = b.addInputNeuron(index);
        return new SignatureGenome(b.getGenome(), in.getInnovationId());
    }

    /**
     * Replace CrossoverService.random with a seedable instance for reproducibility.
     */
    private static CrossoverService<TestCreature> serviceWithSeed(long seed) throws Exception {
        CrossoverService<TestCreature> xsv = new CrossoverService<>();
        Field rnd = CrossoverService.class.getDeclaredField("random");
        rnd.setAccessible(true);
        rnd.set(xsv, new Random(seed));
        return xsv;
    }

    /**
     * Count offspring carrying a given signature neuron.
     */
    private static long countWithSignature(List<TestCreature> list, int neuronId) {
        return list.stream().filter(c -> c.getGenome().getNeuronById(neuronId) != null).count();
    }

    @Test
    @DisplayName("Returns empty list when population ≤ 0")
    void emptyPopulationEarlyExit() throws Exception {
        NeatContext<TestCreature> ctx = new NeatContext<>(new DummyFactory());
        CrossoverService<TestCreature> xsv = serviceWithSeed(0);
        assertTrue(xsv.createOffspring(ctx, 0).isEmpty());
        assertTrue(xsv.createOffspring(ctx, -5).isEmpty());
    }

    @Test
    @DisplayName("With all-zero fitness, offspring come exclusively from first species")
    void zeroFitnessAllotment() throws Exception {
        SignatureGenome g1 = genomeWithSignature(101);
        SignatureGenome g2 = genomeWithSignature(202);
        Species<TestCreature> first = new Species<>(new TestCreature(g1.genome, 0));
        Species<TestCreature> second = new Species<>(new TestCreature(g2.genome, 0));

        NeatConfiguration cfg = new NeatConfiguration();
        cfg.reproduceWithoutCrossover = 1.0;     // purely asexual
        cfg.interspeciesCrossover = 0.0;

        NeatContext<TestCreature> ctx = new NeatContext<>(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutationService(cfg, ctx.innovationService);
        ctx.species.addAll(List.of(first, second));

        CrossoverService<TestCreature> xsv = serviceWithSeed(1);
        List<TestCreature> off = xsv.createOffspring(ctx, 6);

        assertEquals(6, off.size(), "should create the requested number of offspring");
        assertEquals(6, countWithSignature(off, g1.innovationId), "all offspring should inherit from first species");
        assertEquals(0, countWithSignature(off, g2.innovationId), "no offspring should come from second species");
    }

    @Test
    @DisplayName("Offspring quota follows relative fitness proportions")
    void weightedQuotaDistribution() throws Exception {
        SignatureGenome strongSig = genomeWithSignature(111);
        SignatureGenome weakSig = genomeWithSignature(222);
        Species<TestCreature> strong = new Species<>(new TestCreature(strongSig.genome, 30)); // fitness 30
        Species<TestCreature> weak = new Species<>(new TestCreature(weakSig.genome, 10)); // fitness 10

        NeatConfiguration cfg = new NeatConfiguration();
        cfg.reproduceWithoutCrossover = 1.0;
        cfg.interspeciesCrossover = 0.0;

        NeatContext<TestCreature> ctx = new NeatContext<TestCreature>(new DummyFactory(), cfg);
        ctx.mutationService = new NoOpMutationService(cfg, ctx.innovationService);
        ctx.species.addAll(List.of(strong, weak));

        CrossoverService<TestCreature> xsv = serviceWithSeed(42);
        int population = 8;                       // total offspring requested
        List<TestCreature> off = xsv.createOffspring(ctx, population);

        assertEquals(population, off.size(), "offspring count mismatch");

        long strongKids = countWithSignature(off, strongSig.innovationId);
        long weakKids = countWithSignature(off, weakSig.innovationId);

        assertEquals(6, strongKids, "strong species should receive 6 offspring (¾ of pop)");
        assertEquals(2, weakKids, "weak species should receive 2 offspring (¼ of pop)");
        assertEquals(population, strongKids + weakKids);
    }
}