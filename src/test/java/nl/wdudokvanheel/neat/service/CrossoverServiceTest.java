package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Crossover invariants.
 * <p>
 * Parent F (fit)   : Neurons 1,2,3  | Connections 1,2,3
 * Parent W (weak)  : Neurons 1,2,    | Connections 1,2
 * <p>
 * • Genes 1 & 2   → matching
 * • Gene 3        → excess (fit parent only)
 * <p>
 * All connections are enabled, so we don't hit the 75 % disabled rule
 * and keep the test deterministic.
 */
class CrossoverServiceTest {

    private final InnovationService inv = new InnovationService();
    private final CrossoverService xsv = new CrossoverService();

    private Genome buildFitParent() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);

        b.addConnection(in, hid).setWeight(0.7);
        b.addConnection(hid, out).setWeight(-1.1);
        b.addConnection(in, out).setWeight(0.3);   // excess for weak parent

        return b.getGenome();
    }

    private Genome buildWeakParent() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);

        b.addConnection(in, hid).setWeight(0.9);   // weight differs
        b.addConnection(hid, out).setWeight(-1.1);

        return b.getGenome();
    }

    @Test
    @DisplayName("Child contains fit-parent neurons + matching only")
    void neuronInheritance() {

        Genome child = xsv.crossover(buildFitParent(), buildWeakParent());

        Set<Integer> childIds = child.getNeurons().stream()
                .map(NeuronGene::getInnovationId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(1, 2, 3), childIds,
                "Child neuron set differs from fit parent's neurons");
    }

    @Test
    @DisplayName("Child inherits all fit-parent connections plus matching")
    void connectionInheritance() {

        Genome fit = buildFitParent();
        Genome weak = buildWeakParent();
        Genome child = xsv.crossover(fit, weak);

        // innovation ids present in the child
        Set<Integer> childConnIds = child.getConnections().stream()
                .map(ConnectionGene::getInnovationId)
                .collect(Collectors.toSet());

        Set<Integer> expected = fit.getConnections().stream()
                .map(ConnectionGene::getInnovationId)
                .collect(Collectors.toSet());

        assertEquals(expected, childConnIds,
                "Child should contain matching and excess from fitter parent only");
    }

    @Test
    @DisplayName("Matching gene weight comes from either parent (50-50)")
    void matchingGeneWeightSource() {

        Genome fit = buildFitParent();
        Genome weak = buildWeakParent();
        Genome child = xsv.crossover(fit, weak);

        ConnectionGene template = fit.getConnection(
                fit.getInputNeurons().getFirst().getInnovationId(),
                fit.getHiddenNeurons().getFirst().getInnovationId());
        ConnectionGene conn1 = child.getConnectionById(template.getInnovationId());
        double wFit = 0.7;
        double wWeak = 0.9;

        assertTrue(conn1.getWeight() == wFit || conn1.getWeight() == wWeak,
                "Weight of matching gene not taken from either parent");
    }

    /* --------------------------------------------------------------------
          Disabled inheritance rule: if gene disabled in either parent, child
          copies disabled flag, then MAY re-enable with 25 % chance.
          Here we disable matching gene #2 in weak parent only.  Child must
          *sometimes* enable, but never enable if both parents had it disabled.
     -------------------------------------------------------------------- */
    @Test
    @DisplayName("If at least one parent has gene disabled, child copies flag then may keep it disabled")
    void disabledFlagInheritance() {

        Genome fit = buildFitParent();
        Genome weak = buildWeakParent();

        ConnectionGene hidOut = weak.getConnection(
                weak.getHiddenNeurons().getFirst().getInnovationId(),
                weak.getOutputNeurons().getFirst().getInnovationId());
        hidOut.setEnabled(false);

        int enabled = 0;
        int disabled = 0;

        // stochastic rule (25 % chance to re-enable); sample 200 times
        for (int i = 0; i < 200; i++) {
            Genome child = xsv.crossover(fit, weak);
            if (child.getConnectionById(hidOut.getInnovationId()).isEnabled()) {
                enabled++;
            } else {
                disabled++;
            }
        }

        assertTrue(enabled > 0, "Child never re-enabled the gene across 200 samples");
        assertTrue(disabled > 0, "Child never kept the gene disabled across 200 samples");
    }

    @Test
    @DisplayName("Crossing identical genomes yields an exact clone")
    void identicalParentsProduceClone() {

        Genome parent = buildFitParent();
        Genome child = xsv.crossover(parent, parent);

        // Compare by innovation-id and weight & enabled
        assertEquals(parent.getConnections().size(), child.getConnections().size(),
                "Size mismatch");

        for (ConnectionGene c : parent.getConnections()) {
            ConnectionGene c2 = child.getConnectionById(c.getInnovationId());
            assertNotNull(c2, "Missing connection in child: " + c.getInnovationId());
            assertEquals(c.getWeight(), c2.getWeight(), 1e-12, "Weight differs");
            assertEquals(c.isEnabled(), c2.isEnabled(), "Enabled flag differs");
        }
    }
}
