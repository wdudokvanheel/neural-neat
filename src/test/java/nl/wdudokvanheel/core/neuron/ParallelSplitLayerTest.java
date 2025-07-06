package nl.wdudokvanheel.core.neuron;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
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
        Genome g = new Genome();

        // 2 inputs on layer 0
        int in0 = innovation.getInputNodeInnovationId(0);
        int in1 = innovation.getInputNodeInnovationId(1);
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, in0, 0),
                new NeuronGene(NeuronGeneType.INPUT, in1, 0)
        );

        // 2 outputs on layer 1
        int out0 = innovation.getOutputNodeInnovationId(0);
        int out1 = innovation.getOutputNodeInnovationId(1);
        g.addNeurons(
                new NeuronGene(NeuronGeneType.OUTPUT, out0, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, out1, 1)
        );

        // parallel connections
        ConnectionGene c0 = new ConnectionGene(innovation.getConnectionInnovationId(in0, out0), in0, out0, 1.0);
        ConnectionGene c1 = new ConnectionGene(innovation.getConnectionInnovationId(in1, out1), in1, out1, 1.0);
        g.addConnections(c0, c1);

        // ACT – split both connections deterministically
        AddNeuronMutation mut = new AddNeuronMutation(innovation);
        mut.replaceConnectionWithNeuron(g, c0);
        mut.replaceConnectionWithNeuron(g, c1);

        // COLLECT layers
        List<NeuronGene> inputs  = g.getNeurons().stream().filter(n -> n.getType() == NeuronGeneType.INPUT).toList();
        List<NeuronGene> hidden = g.getNeurons().stream().filter(n -> n.getType() == NeuronGeneType.HIDDEN).toList();
        List<NeuronGene> outputs = g.getNeurons().stream().filter(n -> n.getType() == NeuronGeneType.OUTPUT).toList();

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
        Genome g = new Genome();

        // inputs (layer 0)
        int in0 = innovation.getInputNodeInnovationId(0);
        int in1 = innovation.getInputNodeInnovationId(1);
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, in0, 0),
                new NeuronGene(NeuronGeneType.INPUT, in1, 0)
        );

        // outputs (layer 1)
        int out0 = innovation.getOutputNodeInnovationId(0);
        int out1 = innovation.getOutputNodeInnovationId(1);
        g.addNeurons(
                new NeuronGene(NeuronGeneType.OUTPUT, out0, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, out1, 1)
        );

        // direct connections
        ConnectionGene c0 = new ConnectionGene(innovation.getConnectionInnovationId(in0, out0), in0, out0, 1.0);
        ConnectionGene c1 = new ConnectionGene(innovation.getConnectionInnovationId(in1, out1), in1, out1, 1.0);
        g.addConnections(c0, c1);

        AddNeuronMutation mut = new AddNeuronMutation(innovation);

        // *** Generation N: split first connection ***
        mut.replaceConnectionWithNeuron(g, c0);

        // check intermediate state
        NeuronGene hidden0 = g.getNeurons().stream()
                .filter(n -> n.getType() == NeuronGeneType.HIDDEN)
                .findFirst().orElseThrow();
        assertEquals(1, hidden0.getLayer(), "first hidden node on layer 1");
        assertEquals(2, g.getNeuronById(out0).getLayer(), "out0 shifted to layer 2");
        assertEquals(2, g.getNeuronById(out1).getLayer(), "out1 already shifted to layer 2");

        // *** Generation N+1: split second connection ***
        mut.replaceConnectionWithNeuron(g, c1);

        // collect hidden nodes
        List<NeuronGene> hiddens = g.getNeurons().stream()
                .filter(n -> n.getType() == NeuronGeneType.HIDDEN)
                .toList();

        // ASSERT: both hidden nodes share layer 1; outputs remain on layer 2
        assertEquals(2, hiddens.size(), "two hidden nodes expected");
        hiddens.forEach(h -> assertEquals(1, h.getLayer(), "hidden nodes should share layer 1"));
        g.getNeurons().stream()
                .filter(n -> n.getType() == NeuronGeneType.OUTPUT)
                .forEach(o -> assertEquals(2, o.getLayer(), "outputs stay on layer 2"));
    }
}