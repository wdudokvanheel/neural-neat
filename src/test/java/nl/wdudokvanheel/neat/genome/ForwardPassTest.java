package nl.wdudokvanheel.neat.genome;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.InputNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.OutputNeuronGene;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.network.Network;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.lang.Math.exp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Feed-forward evaluation & reset semantics
 * <p>
 * Network topology used in all tests:
 * Input #1  (layer 0)
 * │ weight W
 * Output #2 (layer 1, sigmoid activation)
 * <p>
 * Weight W is chosen as 2.0 so that we can calculate the
 * exact sigmoid output by hand and compare for determinism.
 */
class ForwardPassTest {

    /**
     * Build a minimal one-edge genome.
     */
    private static Genome singleEdgeGenome(double weight, InnovationService inv) {

        int inId = inv.getInputNodeInnovationId(0);
        int outId = inv.getOutputNodeInnovationId(0);

        Genome g = new Genome();
        g.addNeuron(new InputNeuronGene(inId, 0));
        g.addNeuron(new OutputNeuronGene(outId, 1));

        int connId = inv.getConnectionInnovationId(inId, outId);
        g.addConnection(new ConnectionGene(connId, inId, outId, weight));

        return g;
    }

    /**
     * Sigmoid as implemented by SigmoidFunction (slope factor 4.9).
     */
    private static double sigmoid(double x) {
        return 1.0 / (1.0 + exp(-4.9 * x));
    }

    @Test
    @DisplayName("Forward pass is deterministic; reset refreshes cached values")
    void deterministicForwardAndReset() {
        InnovationService inv = new InnovationService();
        final double W = 2.0;
        Network net = new Network(singleEdgeGenome(W, inv));

        // FIRST evaluation with input = 1.0
        net.setInput(1.0);
        double out1 = net.getOutput();
        double expected1 = sigmoid(W * 1.0);
        assertEquals(expected1, out1, 1e-10, "initial output wrong");

        // Re-read without reset or input change → cache returns same value
        double outRepeat = net.getOutput();
        assertEquals(out1, outRepeat, 1e-15,
                "second read should hit cached value");

        // Change input but DO NOT reset – output must stay cached
        net.setInput(0.0);
        double outStale = net.getOutput();
        assertEquals(out1, outStale, "output changed without reset – caching broken?");

        // Reset values, set new input, expect fresh computation (sigmoid(0)=0.5)
        net.resetNeuronValues();
        net.setInput(0.0);
        double out2 = net.getOutput();
        assertEquals(0.5, out2, 1e-10, "reset did not clear caches");
    }

    @Test
    @DisplayName("Calling getOutput without setting inputs throws IllegalStateException")
    void missingInputThrows() {
        InnovationService inv = new InnovationService();
        Network net = new Network(singleEdgeGenome(1.0, inv));

        // Do not call setInput(...)
        assertThrows(IllegalStateException.class,
                net::getOutput,
                "expected IllegalStateException for unset input");
    }
}
