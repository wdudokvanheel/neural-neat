package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.NeuronGene;
import nl.wdudokvanheel.neural.neat.genome.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.service.SpeciationService;
import nl.wdudokvanheel.neural.util.AbstractCreature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpeciationServiceTest {

    private static class TestCreature extends AbstractCreature {
        public TestCreature(Genome g, double fit) {
            super(g);
            setFitness(fit);
        }
    }

    private static Genome baseGenome(double w1, double w2) {
        Genome g = new Genome();
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, 1, 0),
                new NeuronGene(NeuronGeneType.INPUT, 2, 0),
                new NeuronGene(NeuronGeneType.OUTPUT, 3, 1)
        );
        g.addConnections(
                new ConnectionGene(1, 1, 3, w1, true),
                new ConnectionGene(2, 2, 3, w2, true)
        );
        return g;
    }

    private static NeatConfiguration cfg(double thresh) {
        NeatConfiguration c = new NeatConfiguration();
        c.speciesThreshold = thresh;
        c.adjustSpeciesThreshold = false;
        return c;
    }

    @Test
    @DisplayName("Similar genomes group; dissimilar form new species")
    void speciationThreshold() {

        double threshold = 0.2;                     // tighter
        SpeciationService svc = new SpeciationService(cfg(threshold));

        Genome gA = baseGenome(0.5, 0.1);          // rep
        Genome gB = baseGenome(0.4, 0.1);          // very similar
        Genome gC = gA.clone();                     // add one excess conn
        gC.addConnection(new ConnectionGene(3, 1, 2, 0.3, true));

        TestCreature A = new TestCreature(gA, 1);
        TestCreature B = new TestCreature(gB, 1);
        TestCreature C = new TestCreature(gC, 1);

        List<Species> out = svc.speciate(List.of(A, B, C), new ArrayList<>());

        assertEquals(2, out.size(), "expected 2 species");

        Species sAB = A.getSpecies();
        assertSame(sAB, B.getSpecies(), "A & B should be together");
        assertNotSame(sAB, C.getSpecies(), "C should be separate");
    }

    @Test
    @DisplayName("sortSpeciesByScore orders by descending fitness")
    void sortSpeciesByFitness() {
        SpeciationService svc = new SpeciationService(cfg(0.5));

        Species low = new Species(new TestCreature(baseGenome(0.5, 0.1), 1));
        Species mid = new Species(new TestCreature(baseGenome(0.5, 0.1), 10));
        Species high = new Species(new TestCreature(baseGenome(0.5, 0.1), 20));

        List<Species> sorted = svc.sortSpeciesByScore(List.of(low, mid, high));

        assertEquals(List.of(high, mid, low), sorted,
                "species not sorted by descending average fitness");
    }

    @Test
    @DisplayName("eliminateStagnantSpecies removes only stagnant ones")
    void eliminateStagnant() {
        NeatConfiguration c = cfg(0.5);
        c.eliminateStagnantSpecies = true;
        SpeciationService svc = new SpeciationService(c);

        Species young = new Species(new TestCreature(baseGenome(0.5, 0.1), 1));
        young.lastImprovement = 0;
        young.lastFitness = 1;

        Species mid = new Species(new TestCreature(baseGenome(0.5, 0.1), 1));
        mid.lastImprovement = 14;
        mid.lastFitness = 1;

        Species old = new Species(new TestCreature(baseGenome(0.5, 0.1), 1));
        old.lastImprovement = 15;          // >= 15 -> removed
        old.lastFitness = 1;

        List<Species> list = new ArrayList<>(List.of(young, mid, old));
        svc.eliminateStagnantSpecies(list);

        assertEquals(Set.of(young),
                new HashSet<>(list),
                "stagnant species elimination incorrect");
    }
}