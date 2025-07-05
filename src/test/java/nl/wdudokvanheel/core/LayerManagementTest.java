package nl.wdudokvanheel.core;

import nl.wdudokvanheel.neural.core.Network;
import nl.wdudokvanheel.neural.core.neuron.Neuron;
import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.mutation.AddConnectionMutation;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class LayerManagementTest {

    @Test
    @DisplayName("Assigning layers when adding neurons & connections")
    void layerAssignmentFromGenome() {
        Genome g = new Genome();
        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, 1, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.HIDDEN, 3, 1));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, 2, 2));
        g.addConnection(new ConnectionGene(1, 1, 3, 1.0));
        g.addConnection(new ConnectionGene(2, 3, 2, 1.0));

        Network net = new Network(g);

        assertEquals(0, net.getInputNeuron(0).layer, "input layer");
        assertEquals(1, net.hiddenNeurons.getFirst().layer, "hidden layer");
        assertEquals(2, net.getOutputNeuron(0).layer, "output layer");
        assertEquals(2, net.getLayers(), "network depth");

        Network clone = net.clone();
        assertEquals(0, clone.getInputNeuron(0).layer);
        assertEquals(1, clone.hiddenNeurons.get(0).layer);
        assertEquals(2, clone.getOutputNeuron(0).layer);
    }

    @Test
    @DisplayName("Stacking hidden layers by consecutively splitting successor edge")
    void stackedLayersOnSuccessiveSplits() {
        InnovationService inv = new InnovationService();
        Genome g = new Genome();

        int in = inv.getInputNodeInnovationId(0);  // layer 0
        int out = inv.getOutputNodeInnovationId(0); // layer 1

        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out, 1));
        ConnectionGene firstEdge = new ConnectionGene(inv.getConnectionInnovationId(in, out), in, out, 1.0);
        g.addConnection(firstEdge);

        AddNeuronMutation mut = new AddNeuronMutation(inv);

        // ── 1st split: in → out  ───────────────────────────────
        mut.replaceConnectionWithNeuron(g, firstEdge);

        // identify the new hidden node & its edge to the output
        NeuronGene h1 = g.getNeurons().stream()
                .filter(n -> n.getType() == NeuronGeneType.HIDDEN)
                .findFirst().get();
        ConnectionGene edgeH1toOut = g.getConnection(h1.getInnovationId(), out);

        // ── 2nd split: h1 → out (successor edge) ───────────────
        mut.replaceConnectionWithNeuron(g, edgeH1toOut);

        // collect layers
        List<Integer> hiddenLayers = g.getNeurons().stream()
                .filter(n -> n.getType() == NeuronGeneType.HIDDEN)
                .map(NeuronGene::getLayer)
                .sorted()
                .toList();

        assertEquals(List.of(1, 2), hiddenLayers,
                "hidden nodes should occupy layers 1 and 2");
        assertEquals(3, g.getNeuronById(out).getLayer(),
                "output should now be on layer 3");
    }

    @Test
    @DisplayName("Shifting affects outputs that sit deeper than the split")
    void shiftMovesDeeperOutputs() {
        InnovationService inv = new InnovationService();
        Genome g = new Genome();

        int in = inv.getInputNodeInnovationId(0);   // L0
        int hid = inv.getNeuronInnovationId(1);      // user-defined hidden at L2
        int out = inv.getOutputNodeInnovationId(0);  // L1

        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.HIDDEN, hid, 2));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out, 1));

        g.addConnections(
                connect(inv, in, out),
                connect(inv, out, hid)         // output → deeper hidden connection (feed-forward)
        );

        // split input→output (gap must be created; both output & deeper hidden shift)
        new AddNeuronMutation(inv)
                .replaceConnectionWithNeuron(g, g.getConnection(in, out));

        assertEquals(2, g.getNeuronById(out).getLayer(), "output shifted from 1 → 2");
        assertEquals(3, g.getNeuronById(hid).getLayer(), "deep hidden shifted from 2 → 3");
    }

    @Test
    @DisplayName("AddConnectionMutation respects layer order")
    void addConnectionRespectsLayers() {
        InnovationService inv = new InnovationService();
        Genome g = new Genome();

        // minimal 3-layer net (0->1->2)
        int in = inv.getInputNodeInnovationId(0);
        int hid = inv.getNeuronInnovationId(1);
        int out = inv.getOutputNodeInnovationId(0);

        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.HIDDEN, hid, 1));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out, 2));
        g.addConnections(
                connect(inv, in, hid),
                connect(inv, hid, out)
        );

        // attempt to add many random connections
        AddConnectionMutation addMut = new AddConnectionMutation(inv);
        for (int i = 0; i < 100; i++) {
            addMut.mutate(g);
        }

        // post-condition: every connection must satisfy source.layer < target.layer
        for (ConnectionGene c : g.getConnections()) {
            int srcLayer = g.getNeuronById(c.getSource()).getLayer();
            int tgtLayer = g.getNeuronById(c.getTarget()).getLayer();
            assertTrue(srcLayer < tgtLayer,
                    () -> "Found invalid connection " + c + " (layers " + srcLayer + "→" + tgtLayer + ")");
        }
    }

    @Test
    @DisplayName("getLayers equals deepest neuron layer")
    void getLayersMatchesMaxLayer() {
        InnovationService inv = new InnovationService();
        Genome g = new Genome();

        // input 0, output 1
        int in = inv.getInputNodeInnovationId(0);
        int out = inv.getOutputNodeInnovationId(0);
        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out, 1));
        g.addConnection(connect(inv, in, out));

        // split five times to deepen network
        AddNeuronMutation mut = new AddNeuronMutation(inv);
        for (int i = 0; i < 5; i++) {
            mut.replaceConnectionWithNeuron(g, g.getConnection(in, out));
        }

        Network net = new Network(g);

        int maxLayer = net.getAllNeurons().stream()
                .mapToInt(n -> n.layer).max().orElseThrow();
        assertEquals(maxLayer, net.getLayers());
    }

    @Test
    @DisplayName("Network.clone() keeps layers and connections")
    void cloneKeepsLayersAndConnections() {
        InnovationService inv = new InnovationService();
        Genome g = new Genome();

        int in = inv.getInputNodeInnovationId(0);
        int hid1 = inv.getNeuronInnovationId(1);
        int hid2 = inv.getNeuronInnovationId(2);
        int out = inv.getOutputNodeInnovationId(0);

        g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in, 0));
        g.addNeuron(new NeuronGene(NeuronGeneType.HIDDEN, hid1, 1));
        g.addNeuron(new NeuronGene(NeuronGeneType.HIDDEN, hid2, 2));
        g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out, 3));

        g.addConnections(
                connect(inv, in, hid1),
                connect(inv, hid1, hid2),
                connect(inv, hid2, out)
        );

        Network n1 = new Network(g);
        Network n2 = n1.clone();

        // per-neuron layer equality
        for (Neuron orig : n1.getAllNeurons()) {
            assertEquals(orig.layer,
                    n2.getNeuronById(orig.getId()).layer,
                    "layer mismatch on neuron #" + orig.getId());
        }

        // connection equality by (srcId, tgtId)
        Set<String> edges1 = edgeSet(n1);
        Set<String> edges2 = edgeSet(n2);
        assertEquals(edges1, edges2, "connection sets differ");
    }

    /**
     * Helper: each edge as "srcId->tgtId".
     */
    private static Set<String> edgeSet(Network net) {
        Set<String> s = new HashSet<>();
        net.getAllNeurons().forEach(n ->
                n.inputs.forEach(c ->
                        s.add(c.source.getId() + "->" + n.getId())));
        return s;
    }

    @Test
    @DisplayName("Parallel splits yield identical layer layout regardless of order")
    void orderIndependenceParallelSplits() {
        int[][] orders = {{0, 1}, {1, 0}};     // two execution sequences

        List<List<Integer>> hiddenLayerResults = new ArrayList<>();

        for (int[] order : orders) {
            InnovationService inv = new InnovationService();
            Genome g = new Genome();

            int[] in = {inv.getInputNodeInnovationId(0),
                    inv.getInputNodeInnovationId(1)};
            int[] out = {inv.getOutputNodeInnovationId(0),
                    inv.getOutputNodeInnovationId(1)};

            g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in[0], 0));
            g.addNeuron(new NeuronGene(NeuronGeneType.INPUT, in[1], 0));
            g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out[0], 1));
            g.addNeuron(new NeuronGene(NeuronGeneType.OUTPUT, out[1], 1));
            ConnectionGene[] c = {
                    connect(inv, in[0], out[0]),
                    connect(inv, in[1], out[1])
            };
            g.addConnections(c);

            AddNeuronMutation mut = new AddNeuronMutation(inv);
            // perform two splits in specified order
            mut.replaceConnectionWithNeuron(g, c[order[0]]);
            mut.replaceConnectionWithNeuron(g, c[order[1]]);

            hiddenLayerResults.add(layers(g, NeuronGeneType.HIDDEN));
        }

        assertEquals(hiddenLayerResults.get(0), hiddenLayerResults.get(1),
                "Layer layout differs with split order");
    }

    @Test
    @DisplayName("Random fuzz: never generate backward edges")
    void fuzzPropertyTest() {
        final int ITERATIONS = 50;
        final Random rnd = new Random();

        for (int iter = 0; iter < ITERATIONS; iter++) {
            InnovationService inv = new InnovationService();
            Genome g = new Genome();

            // 2-input, 2-output starter
            int in0 = inv.getInputNodeInnovationId(0);
            int in1 = inv.getInputNodeInnovationId(1);
            int out0 = inv.getOutputNodeInnovationId(0);
            int out1 = inv.getOutputNodeInnovationId(1);

            g.addNeurons(
                    new NeuronGene(NeuronGeneType.INPUT, in0, 0),
                    new NeuronGene(NeuronGeneType.INPUT, in1, 0),
                    new NeuronGene(NeuronGeneType.OUTPUT, out0, 1),
                    new NeuronGene(NeuronGeneType.OUTPUT, out1, 1)
            );
            g.addConnections(
                    connect(inv, in0, out0),
                    connect(inv, in1, out1)
            );

            AddNeuronMutation addNeu = new AddNeuronMutation(inv);
            AddConnectionMutation addConn = new AddConnectionMutation(inv);

            // perform random sequence of 20 mutations
            for (int step = 0; step < 20; step++) {
                if (rnd.nextBoolean()) {
                    // split a random connection
                    List<ConnectionGene> list = new ArrayList<>(g.getActiveConnections());
                    if (!list.isEmpty()) {
                        ConnectionGene c = list.get(rnd.nextInt(list.size()));
                        addNeu.replaceConnectionWithNeuron(g, c);
                    }
                } else {
                    addConn.mutate(g);
                }
            }

            // post-condition: strict layer order on every edge
            for (ConnectionGene c : g.getConnections()) {
                NeuronGene src = g.getNeuronById(c.getSource());
                NeuronGene tgt = g.getNeuronById(c.getTarget());
                assertTrue(src.getLayer() < tgt.getLayer(),
                        () -> "Backward edge found after fuzz (" + src.getLayer() + "→" + tgt.getLayer() + ")");
            }
        }
    }

    /**
     * Convenience: create a weight-1 connection with a unique innovation-id.
     */
    private static ConnectionGene connect(InnovationService inv, int src, int tgt) {
        return new ConnectionGene(inv.getConnectionInnovationId(src, tgt), src, tgt, 1.0);
    }

    /**
     * Collect layers of neurons of a given type, sorted ascending.
     */
    private static List<Integer> layers(Genome g, NeuronGeneType t) {
        return g.getNeurons().stream()
                .filter(n -> n.getType() == t)
                .map(NeuronGene::getLayer)
                .sorted()
                .collect(Collectors.toList());
    }
}
