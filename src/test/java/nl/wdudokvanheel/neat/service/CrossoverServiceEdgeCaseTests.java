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

class CrossoverServiceEdgeCaseTests {

    private final CrossoverService xsv = new CrossoverService();

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Genome bothDisabledParents() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in, hid).setWeight(0.8);
        b.addConnection(hid, out).setWeight(-1.2);
        b.getGenome().getConnections().forEach(c -> c.setEnabled(false));
        return b.getGenome();
    }

    private Genome weakParentWithExtraNeuron() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene h1 = b.addHiddenNeuron(0);
        HiddenNeuronGene h2 = b.addHiddenNeuron(1); // disjoint neuron
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in, h1).setWeight(0.5);
        b.addConnection(h1, out).setWeight(0.5);
        b.addConnection(h2, out).setWeight(0.9); // disjoint
        return b.getGenome();
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
        int disjointNeuron = weak.getHiddenNeurons().get(1).getInnovationId();
        boolean leaked = child.getConnections().stream()
                .anyMatch(c -> c.getSource() == disjointNeuron || c.getTarget() == disjointNeuron);
        assertFalse(leaked, "Disjoint connection from weak parent leaked into child");
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
