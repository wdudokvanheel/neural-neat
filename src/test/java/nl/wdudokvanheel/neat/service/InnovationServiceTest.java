package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression-suite for InnovationService.
 * <p>
 * Contracts defended
 * ────────────────────────────────────────────────────────────
 * 1.  Same *structural event*  →   same innovation-id            (determinism)
 * 2.  Different event         →   unique, monotonically increasing IDs
 * 3.  Thread-safety (optional): no duplicate IDs under parallel access
 */
class InnovationServiceTest {

    @Test
    @DisplayName("Same connection split returns same hidden-neuron id")
    void hiddenNeuronIdDeterminism() {
        InnovationService inv = new InnovationService();

        int connId = 42;                           // arbitrary
        int first = inv.getHiddenNeuronInnovationId(connId);
        int second = inv.getHiddenNeuronInnovationId(connId);

        assertEquals(first, second,
                "Splitting same connection twice yielded different neuron IDs");
    }

    @Test
    @DisplayName("Same (source,target) pair returns same connection id")
    void connectionIdDeterminism() {
        InnovationService inv = new InnovationService();

        int src = 10, tgt = 20;
        int id1 = inv.getConnectionInnovationId(src, tgt);
        int id2 = inv.getConnectionInnovationId(src, tgt);

        assertEquals(id1, id2,
                "Same (src,tgt) received two different connection IDs");
    }

    @Test
    @DisplayName("Different structural events produce strictly increasing ids")
    void idsAreUniqueAndIncreasing() {
        InnovationService inv = new InnovationService();

        int a = inv.getConnectionInnovationId(1, 2);   // first connection
        int b = inv.getConnectionInnovationId(2, 3);   // second connection
        int n = inv.getHiddenNeuronInnovationId(a);          // first hidden neuron

        assertTrue(a < b, "IDs not increasing for successive connections");
        assertTrue(b < n, "Neuron ID should follow connection IDs");
    }

    @Test
    @DisplayName("Two genomes using same service share innovation space")
    void sharedServiceGivesConsistentIdsAcrossGenomes() {
        InnovationService inv = new InnovationService();

        // Genome A splits connection X
        int connX = inv.getConnectionInnovationId(5, 6);
        int neuronA = inv.getHiddenNeuronInnovationId(connX);

        // Genome B (later) splits *the same* connection X
        int neuronB = inv.getHiddenNeuronInnovationId(connX);

        assertEquals(neuronA, neuronB,
                "Same structural event in different genomes produced different ids");
    }

    // ────────────────────────────────────────────────────────────
    // Thread-safety smoke test, spawns N threads each requesting
    //    a unique connection; asserts there are no duplicate ids.
    // ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Parallel access generates no duplicate ids")
    void noDuplicatesUnderParallelAccess() throws Exception {
        final int THREADS = 16;
        final int REQUESTS_PER_THREAD = 1_000;

        InnovationService inv = new InnovationService();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        Set<Integer> idSet = ConcurrentHashMap.newKeySet();
        Callable<Void> task = () -> {
            for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
                // each thread uses disjoint source/target to avoid overlap
                int src = (int) (Thread.currentThread().getId() * 10_000 + i);
                int tgt = src + 1;
                int id = inv.getConnectionInnovationId(src, tgt);
                if (!idSet.add(id)) {
                    fail("Duplicate id generated in parallel: " + id);
                }
            }
            return null;
        };

        // submit tasks
        var futures = IntStream.range(0, THREADS)
                .mapToObj(i -> pool.submit(task))
                .toList();

        // ensure all complete without exception
        for (var f : futures) f.get();
        pool.shutdownNow();
    }
}
