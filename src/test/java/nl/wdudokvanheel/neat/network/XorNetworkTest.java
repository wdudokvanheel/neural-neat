package nl.wdudokvanheel.neat.network;

import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.HiddenNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.InputNeuronGene;
import nl.wdudokvanheel.neural.neat.genome.OutputNeuronGene;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.network.Network;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XOR sanity check.
 *
 * Network topology (innovation ids shown after '#'):
 *
 *   layer 0                layer 1                layer 2
 * ┌──────────┐          ┌──────────┐          ┌──────────┐
 * │ In A #1  │─┐      ┌─│ Hid0 #4  │─┐      ┌─│ Out #6   │
 * ├──────────┤ │  20  │ ├──────────┤ │  20 │ └──────────┘
 * │ In B #2  │─┼──20──┤ │ Hid1 #5  │─┤  20 │
 * ├──────────┤ │      │ ├──────────┤ │      │
 * │ Bias #3  │─┘ -10  └─│          │─┘ -30  │
 * └──────────┘             ↑ -20 -20 ↓ 30   │
 *
 * Weights are the classic "big +/- weights" trick that
 * produces crisp 0 or 1 outputs through a logistic unit.
 */
class XorNetworkTest {

    private Genome buildXorGenome() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);

        // ─── Neurons ────────────────────────────────────────────────
        InputNeuronGene a     = b.addInputNeuron(0);   // #1
        InputNeuronGene bIn   = b.addInputNeuron(1);   // #2
        InputNeuronGene bias  = b.addInputNeuron(2);   // #3  (constant 1)

        HiddenNeuronGene h0 = b.addHiddenNeuron(1);    // #4
        HiddenNeuronGene h1 = b.addHiddenNeuron(2);    // #5
        OutputNeuronGene out = b.addOutputNeuron(0);   // #6

        // ─── Connections with hand-tuned weights ───────────────────
        // In A / B → Hid0  (20, 20, -10)
        b.addConnection(a, h0).setWeight(20);
        b.addConnection(bIn, h0).setWeight(20);
        b.addConnection(bias, h0).setWeight(-10);

        // In A / B → Hid1  (-20, -20, 30)
        b.addConnection(a, h1).setWeight(-20);
        b.addConnection(bIn, h1).setWeight(-20);
        b.addConnection(bias, h1).setWeight(30);

        // Hid0 / Hid1 → Out  (20, 20, -30) — bias reused
        b.addConnection(h0, out).setWeight(20);
        b.addConnection(h1, out).setWeight(20);
        b.addConnection(bias, out).setWeight(-30);

        return b.getGenome();
    }

    @Test
    @DisplayName("Hand-wired XOR network produces ~0 or ~1 for all four input pairs")
    void xorTruthTable() {
        Network net = new Network(buildXorGenome());

        // Truth table: A, B → XOR
        int[][] inputs = { {0,0}, {0,1}, {1,0}, {1,1} };
        int[]   expect = { 0,     1,     1,     0      };

        for (int i = 0; i < inputs.length; i++) {
            int a = inputs[i][0];
            int b = inputs[i][1];

            // reset neurons, set inputs (A,B,Bias=1)
            net.resetNeuronValues();
            net.setInput(a, b, 1);

            double out = net.getOutput();   // single output neuron

            if (expect[i] == 1) {
                assertTrue(out > 0.9, "expected 1 for ("+a+","+b+"), got "+out);
            } else {
                assertTrue(out < 0.1, "expected 0 for ("+a+","+b+"), got "+out);
            }
        }
    }
}
