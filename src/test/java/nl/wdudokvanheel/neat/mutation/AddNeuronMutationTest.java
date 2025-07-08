package nl.wdudokvanheel.neat.mutation;

import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddNeuronMutationTest {
    @Test
    @DisplayName("Output layer should shift when hidden node is added")
    void insertingNeuronShiftsOutputLayer() {
        InnovationService innovation = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(innovation);

        InputNeuronGene in = b.addInputNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);

        ConnectionGene conn = b.addConnection(in, out);
        conn.setWeight(1.0);
        Genome g = b.getGenome();

        new AddNeuronMutation(innovation).mutate(g);

        NeuronGene hidden = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .findFirst()
                .orElseThrow();
        assertEquals(1, hidden.getLayer(), "hidden should occupy the freed-up layer");

        NeuronGene output = g.getNeuronById(out.getInnovationId());
        assertEquals(2, output.getLayer(), "output must be shifted one layer deeper");
    }
}