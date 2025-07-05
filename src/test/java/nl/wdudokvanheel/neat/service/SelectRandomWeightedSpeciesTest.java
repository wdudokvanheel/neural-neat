package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.AbstractCreature;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.Species;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra coverage for CrossoverService.selectRandomWeightedSpecies(…)
 */
class SelectRandomWeightedSpeciesTest {

    /* ---------- helpers -------------------------------------------------- */

    /** Minimal creature that lets us set fitness directly. */
    private static class DummyCreature extends AbstractCreature {
        DummyCreature(double fitness) {
            super(new Genome());
            setFitness(fitness);
        }
    }

    private static Species species(double fitness) {
        return new Species(new DummyCreature(fitness));
    }

    private static Method privateSelector(boolean withExclude) throws Exception {
        return CrossoverService.class.getDeclaredMethod(
                "selectRandomWeightedSpecies",
                withExclude ? new Class[]{List.class, Species.class}
                            : new Class[]{List.class});
    }

    private static CrossoverService serviceWithSeed(long seed) throws Exception {
        CrossoverService xsv = new CrossoverService();
        Field rnd = CrossoverService.class.getDeclaredField("random");
        rnd.setAccessible(true);
        rnd.set(xsv, new Random(seed));
        return xsv;
    }

    /* ---------- tests ---------------------------------------------------- */

    @Test
    @DisplayName("Higher-fitness species is chosen more often")
    void weightedBiasFavorsFitter() throws Exception {
        Species low  = species(1);   // fitness 1
        Species high = species(10);  // fitness 10

        CrossoverService xsv = serviceWithSeed(0);
        Method sel = privateSelector(false);
        sel.setAccessible(true);

        int highHits = 0;
        int lowHits  = 0;
        for (int i = 0; i < 10_000; i++) {
            Species pick = (Species) sel.invoke(xsv, List.of(low, high));
            if (pick == high) { highHits++; } else { lowHits++; }
        }
        assertTrue(highHits > lowHits,
                "expected high-fitness species to be picked more often");
    }

    @Test
    @DisplayName("exclude parameter is strictly respected")
    void excludedSpeciesNeverReturned() throws Exception {
        Species a = species(5);
        Species b = species(5);

        CrossoverService xsv = serviceWithSeed(42);
        Method sel = privateSelector(true);
        sel.setAccessible(true);

        for (int i = 0; i < 5_000; i++) {
            Species pick = (Species) sel.invoke(xsv, List.of(a, b), a);
            assertNotSame(a, pick, "excluded species was returned");
        }
    }

    @Test
    @DisplayName("Fallback path when total fitness ≤ 0")
    void zeroFitnessFallback() throws Exception {
        Species first  = species(0);
        Species second = species(0);

        CrossoverService xsv = serviceWithSeed(7);

        Method selNoEx = privateSelector(false);
        selNoEx.setAccessible(true);
        assertSame(first, selNoEx.invoke(xsv, List.of(first, second)),
                "should return first element when all fitness == 0");

        Method selEx = privateSelector(true);
        selEx.setAccessible(true);
        assertSame(second, selEx.invoke(xsv, List.of(first, second), first),
                "should return second when first is excluded in zero-fitness case");
    }
}
