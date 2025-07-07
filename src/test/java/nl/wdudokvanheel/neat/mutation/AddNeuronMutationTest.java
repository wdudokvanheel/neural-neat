package nl.wdudokvanheel.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddNeuronMutationTest {
    @Test
    @DisplayName("Output layer should shift when hidden node is added")
    void insertingNeuronShiftsOutputLayer() {
        InnovationService innovation = new InnovationService();
        Genome g = new Genome();

        int inId = innovation.getInputNodeInnovationId(0);
        int outId = innovation.getOutputNodeInnovationId(0);

        g.addNeuron(new InputNeuronGene(inId, 0));
        g.addNeuron(new OutputNeuronGene(outId, 1));

        int connId = innovation.getConnectionInnovationId(inId, outId);
        g.addConnection(new ConnectionGene(connId, inId, outId, 1.0));

        new AddNeuronMutation(innovation).mutate(g);

        NeuronGene hidden = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .findFirst()
                .orElseThrow();
        assertEquals(1, hidden.getLayer(), "hidden should occupy the freed-up layer");

        NeuronGene output = g.getNeuronById(outId);
        assertEquals(2, output.getLayer(), "output must be shifted one layer deeper");
    }
}