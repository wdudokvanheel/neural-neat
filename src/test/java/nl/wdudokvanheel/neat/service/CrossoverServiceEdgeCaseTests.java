package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.service.CrossoverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CrossoverServiceEdgeCaseTests {

    private final CrossoverService xsv = new CrossoverService();

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Genome bothDisabledParents() {
        Genome g = new Genome();
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT,  1, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, 2, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, 3, 2)
        );
        g.addConnections(
                new ConnectionGene(1, 1, 2,  0.8, false),
                new ConnectionGene(2, 2, 3, -1.2, false)
        );
        return g;
    }

    private Genome weakParentWithExtraNeuron() {
        Genome g = new Genome();
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT,  1, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, 2, 1),
                new NeuronGene(NeuronGeneType.HIDDEN, 4, 1), // disjoint neuron
                new NeuronGene(NeuronGeneType.OUTPUT, 3, 2)
        );
        g.addConnections(
                new ConnectionGene(1, 1, 2, 0.5, true),           // matching
                new ConnectionGene(2, 2, 3, 0.5, true),           // matching
                new ConnectionGene(5, 4, 3, 0.9, true)            // disjoint
        );
        return g;
    }
    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Disabled in both parents stays disabled in child")
    void disabledGeneRemainsDisabled() {
        Genome fit  = bothDisabledParents();
        Genome weak = bothDisabledParents();

        Genome child = xsv.crossover(fit, weak);

        child.getConnections().forEach(c -> assertFalse(c.isEnabled()));
    }

    @Test
    @DisplayName("Enabled in both parents never disabled in child")
    void enabledGeneRemainsEnabled() {
        Genome fit = bothDisabledParents();
        fit.getConnections().forEach(c -> c.setEnabled(true));

        Genome child = xsv.crossover(fit, fit);

        child.getConnections().forEach(c -> assertTrue(c.isEnabled()));
    }

    @Test
    @DisplayName("Disjoint neuron and edges of weak parent are ignored")
    void weakParentDisjointGenesExcluded() {
        Genome fit  = bothDisabledParents();      // fitter, fewer genes
        Genome weak = weakParentWithExtraNeuron();

        Genome child = xsv.crossover(fit, weak);

        // child must match neuron set of fitter parent only
        Set<Integer> childNeuronIds =
                child.getNeurons().stream().map(NeuronGene::getInnovationId).collect(Collectors.toSet());

        assertEquals(Set.of(1, 2, 3), childNeuronIds);
        assertNull(child.getNeuronById(4), "Disjoint neuron from weak parent leaked into child");

        // and the connection referencing that neuron must be absent
        assertNull(child.getConnectionById(5), "Disjoint connection from weak parent leaked into child");
    }

    @Test
    @DisplayName("All child connections reference neurons that exist in child")
    void noDanglingConnections() {
        Genome fit  = bothDisabledParents();
        Genome weak = weakParentWithExtraNeuron();

        Genome child = xsv.crossover(fit, weak);

        child.getConnections().forEach(c -> {
            assertNotNull(child.getNeuronById(c.getSource()), "Missing source neuron " + c.getSource());
            assertNotNull(child.getNeuronById(c.getTarget()), "Missing target neuron " + c.getTarget());
        });
    }
}
