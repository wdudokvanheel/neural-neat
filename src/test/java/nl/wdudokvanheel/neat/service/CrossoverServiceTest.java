package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
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
        Genome g = new Genome();

        // Neuron innovation ids: 1=input , 2=hidden , 3=output
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, 1, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, 2, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, 3, 2)
        );
        // Connections
        g.addConnections(
                new ConnectionGene(1, 1, 2, 0.7, true),
                new ConnectionGene(2, 2, 3, -1.1, true),
                new ConnectionGene(3, 1, 3, 0.3, true)   // excess for W
        );
        return g;
    }

    private Genome buildWeakParent() {
        Genome g = new Genome();

        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, 1, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, 2, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, 3, 2)
        );
        g.addConnections(
                new ConnectionGene(1, 1, 2, 0.9, true),   // weight differs
                new ConnectionGene(2, 2, 3, -1.1, true)
        );
        return g;
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

        // expected: {1,2,3}.  3 is excess in fit.
        assertEquals(Set.of(1, 2, 3), childConnIds,
                "Child should contain matching and excess from fitter parent only");
    }

    @Test
    @DisplayName("Matching gene weight comes from either parent (50-50)")
    void matchingGeneWeightSource() {

        Genome child = xsv.crossover(buildFitParent(), buildWeakParent());

        ConnectionGene conn1 = child.getConnectionById(1); // matching
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

        // Disable connection 2 only in weak parent
        weak.getConnectionById(2).setEnabled(false);

        int enabled = 0;
        int disabled = 0;

        // stochastic rule (25 % chance to re-enable); sample 200 times
        for (int i = 0; i < 200; i++) {
            Genome child = xsv.crossover(fit, weak);
            if (child.getConnectionById(2).isEnabled()) {
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
