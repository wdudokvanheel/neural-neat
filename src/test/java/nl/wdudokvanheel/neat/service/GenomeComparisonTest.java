package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.HiddenNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.InputNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.OutputNeuronGene;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.GenomeComparison;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression-suite for GenomeComparison.
 * <p>
 * Innovation IDs used
 * Neurons: 1 (input) , 2 (input) , 3 (output) , 4 (hidden)
 * Conns  : 1 (1→3) , 2 (2→3) , 3 (1→4) , 4 (4→3)
 * <p>
 * Genome P  = { neurons 1,2,3 ; connections 1,2 }
 * Genome Q  = P  + connection 4          (excess)
 * Genome R  = P  + connection 3          (disjoint)
 */
class GenomeComparisonTest {
    private static Genome baseGenome() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in1 = b.addInputNeuron(0);
        InputNeuronGene in2 = b.addInputNeuron(1);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in1, out).setWeight(0.5);
        b.addConnection(in2, out).setWeight(-0.5);
        return b.getGenome();
    }

    private static Genome genomeWithExcess() {
        Genome base = baseGenome();
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        // recreate base to use same IDs
        InputNeuronGene in1 = b.addInputNeuron(0);
        InputNeuronGene in2 = b.addInputNeuron(1);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in1, out).setWeight(0.5);
        b.addConnection(in2, out).setWeight(-0.5);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        b.addConnection(hid, out).setWeight(1.0); // excess
        return b.getGenome();
    }

    private static Genome genomeWithDisjoint() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in1 = b.addInputNeuron(0);
        InputNeuronGene in2 = b.addInputNeuron(1);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in1, out).setWeight(0.5);
        b.addConnection(in2, out).setWeight(-0.5);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        b.addConnection(in1, hid).setWeight(1.0); // disjoint connection
        return b.getGenome();
    }

    @Test
    @DisplayName("Distance is symmetrical")
    void symmetry() {
        Genome A = baseGenome();
        Genome B = genomeWithExcess();

        double dAB = new GenomeComparison(A, B).getDistance();
        double dBA = new GenomeComparison(B, A).getDistance();

        assertEquals(dAB, dBA, 1e-12, "distance(A,B) ≠ distance(B,A)");
    }

    @Test
    @DisplayName("Genome distance to itself is zero")
    void zeroSelfDistance() {
        Genome A = baseGenome();
        assertEquals(0.0, new GenomeComparison(A, A).getDistance(), 1e-12);
    }

    @Test
    @DisplayName("Average weight difference computed correctly")
    void weightDifference() {
        // identical except conn1 weight differs
        Genome A = baseGenome();
        Genome B = baseGenome().clone();
        int connId = A.getConnections().getFirst().getInnovationId();
        B.getConnectionById(connId).setWeight(0.1);   // change weight by 0.4

        double expectedAverage = 0.2;              // only 1 matching gene differs
        double c3 = 0.5;                           // default weight coefficient

        double distance = new GenomeComparison(A, B).getDistance();
        assertEquals(c3 * expectedAverage, distance, 1e-12,
                "weight term not applied correctly (no excess/disjoint here)");
    }

    @Test
    @DisplayName("Excess gene contributes C1/N")
    void excessCounting() {
        Genome P = baseGenome();          // 2 connections
        Genome Q = genomeWithExcess();    // 3 connections (one excess)

        int N = 3;                        // max(totalGenes)
        double C1 = 1.0;                  // default excessCoefficient
        double expected = C1 * 1.0 / N;   // one excess

        double d = new GenomeComparison(P, Q).getDistance();
        assertEquals(expected, d, 1e-12,
                "wrong excess contribution");
    }

    @Test
    @DisplayName("Disjoint gene contributes C2/N")
    void disjointCounting() {
        Genome P = baseGenome();            // 2 connections
        Genome R = genomeWithDisjoint();    // 3 connections (one disjoint)

        int N = 3;
        double C2 = 1.0;
        double expected = C2 * 1.0 / N;

        double d = new GenomeComparison(P, R).getDistance();
        assertEquals(expected, d, 1e-12,
                "wrong disjoint contribution");
    }
}
