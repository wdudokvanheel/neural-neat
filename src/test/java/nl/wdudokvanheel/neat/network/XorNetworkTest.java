package nl.wdudokvanheel.neat.network;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.NeuronGene;
import nl.wdudokvanheel.neural.neat.genome.NeuronGeneType;
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

        // ─── Neurons ────────────────────────────────────────────────
        int aId    = inv.getInputNodeInnovationId(0);   // #1
        int bId    = inv.getInputNodeInnovationId(1);   // #2
        int biasId = inv.getInputNodeInnovationId(2);   // #3  (constant 1)

        int h0Id = inv.getNeuronInnovationId(1);        // #4
        int h1Id = inv.getNeuronInnovationId(2);        // #5
        int outId = inv.getOutputNodeInnovationId(0);   // #6

        Genome g = new Genome();

        // inputs & bias on layer 0
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, aId, 0),
                new NeuronGene(NeuronGeneType.INPUT, bId, 0),
                new NeuronGene(NeuronGeneType.INPUT, biasId, 0)
        );
        // hidden layer 1
        g.addNeurons(
                new NeuronGene(NeuronGeneType.HIDDEN, h0Id, 1),
                new NeuronGene(NeuronGeneType.HIDDEN, h1Id, 1)
        );
        // output layer 2
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, outId, 2));

        // ─── Connections with hand-tuned weights ───────────────────
        // In A / B → Hid0  (20, 20, -10)
        g.addConnections(
                new ConnectionGene(inv.getConnectionInnovationId(aId, h0Id), aId, h0Id,  20),
                new ConnectionGene(inv.getConnectionInnovationId(bId, h0Id), bId, h0Id,  20),
                new ConnectionGene(inv.getConnectionInnovationId(biasId, h0Id), biasId, h0Id, -10)
        );
        // In A / B → Hid1  (-20, -20, 30)
        g.addConnections(
                new ConnectionGene(inv.getConnectionInnovationId(aId, h1Id), aId, h1Id, -20),
                new ConnectionGene(inv.getConnectionInnovationId(bId, h1Id), bId, h1Id, -20),
                new ConnectionGene(inv.getConnectionInnovationId(biasId, h1Id), biasId, h1Id,  30)
        );
        // Hid0 / Hid1 → Out  (20, 20, -30) — bias reused
        g.addConnections(
                new ConnectionGene(inv.getConnectionInnovationId(h0Id, outId), h0Id, outId, 20),
                new ConnectionGene(inv.getConnectionInnovationId(h1Id, outId), h1Id, outId, 20),
                new ConnectionGene(inv.getConnectionInnovationId(biasId, outId), biasId, outId, -30)
        );

        return g;
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
