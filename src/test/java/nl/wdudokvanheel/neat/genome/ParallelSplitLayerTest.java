package nl.wdudokvanheel.neat.genome;

import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify that splitting two independent input→output connections
 * places the two new hidden nodes on the *same* intermediate layer and
 * pushes both outputs one layer deeper.
 */
class ParallelSplitLayerTest {

    @Test
    void twoIndependentSplitsShareIntermediateLayer() {
        InnovationService innovation = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(innovation);

        // 2 inputs on layer 0
        InputNeuronGene in0 = b.addInputNeuron(0);
        InputNeuronGene in1 = b.addInputNeuron(1);

        // 2 outputs on layer 1
        OutputNeuronGene out0 = b.addOutputNeuron(0);
        OutputNeuronGene out1 = b.addOutputNeuron(1);

        // parallel connections
        ConnectionGene c0 = b.addConnection(in0, out0);
        ConnectionGene c1 = b.addConnection(in1, out1);
        c0.setWeight(1.0);
        c1.setWeight(1.0);

        Genome g = b.getGenome();

        // ACT – split both connections deterministically
        AddNeuronMutation mut = new AddNeuronMutation(innovation);
        mut.replaceConnectionWithNeuron(g, c0);
        mut.replaceConnectionWithNeuron(g, c1);

        // COLLECT layers
        List<NeuronGene> inputs  = g.getNeurons().stream().filter(n -> n instanceof InputNeuronGene).toList();
        List<NeuronGene> hidden = g.getNeurons().stream().filter(n -> n instanceof HiddenNeuronGene).toList();
        List<NeuronGene> outputs = g.getNeurons().stream().filter(n -> n instanceof OutputNeuronGene).toList();

        // ASSERT – inputs remain on 0
        inputs.forEach(n -> assertEquals(0, n.getLayer(), "inputs stay on layer 0"));

        // both hidden nodes share layer 1
        hidden.forEach(n -> assertEquals(1, n.getLayer(), "hidden nodes should be on layer 1"));
        assertEquals(2, hidden.size(), "exactly two hidden nodes expected");

        // outputs are now on layer 2
        outputs.forEach(n -> assertEquals(2, n.getLayer(), "outputs should shift to layer 2"));
    }

    @Test
    void hiddenNodesShareLayerAcrossGenerations() {
        InnovationService innovation = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(innovation);

        // inputs (layer 0)
        InputNeuronGene in0 = b.addInputNeuron(0);
        InputNeuronGene in1 = b.addInputNeuron(1);

        // outputs (layer 1)
        OutputNeuronGene out0 = b.addOutputNeuron(0);
        OutputNeuronGene out1 = b.addOutputNeuron(1);

        // direct connections
        ConnectionGene c0 = b.addConnection(in0, out0);
        ConnectionGene c1 = b.addConnection(in1, out1);
        c0.setWeight(1.0);
        c1.setWeight(1.0);

        Genome g = b.getGenome();

        AddNeuronMutation mut = new AddNeuronMutation(innovation);

        // *** Generation N: split first connection ***
        mut.replaceConnectionWithNeuron(g, c0);

        // check intermediate state
        NeuronGene hidden0 = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .findFirst().orElseThrow();
        assertEquals(1, hidden0.getLayer(), "first hidden node on layer 1");
        assertEquals(2, g.getNeuronById(out0.getInnovationId()).getLayer(), "out0 shifted to layer 2");
        assertEquals(2, g.getNeuronById(out1.getInnovationId()).getLayer(), "out1 already shifted to layer 2");

        // *** Generation N+1: split second connection ***
        mut.replaceConnectionWithNeuron(g, c1);

        // collect hidden nodes
        List<NeuronGene> hiddens = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .toList();

        // ASSERT: both hidden nodes share layer 1; outputs remain on layer 2
        assertEquals(2, hiddens.size(), "two hidden nodes expected");
        hiddens.forEach(h -> assertEquals(1, h.getLayer(), "hidden nodes should share layer 1"));
        g.getNeurons().stream()
                .filter(n -> n instanceof OutputNeuronGene)
                .forEach(o -> assertEquals(2, o.getLayer(), "outputs stay on layer 2"));
    }
}