package nl.wdudokvanheel.neat.genome;

import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.AddConnectionMutation;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.network.Network;
import nl.wdudokvanheel.neural.network.neuron.Neuron;
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
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);
        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in, hid).setWeight(1.0);
        b.addConnection(hid, out).setWeight(1.0);
        Genome g = b.getGenome();

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
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in = b.addInputNeuron(0);  // layer 0
        OutputNeuronGene out = b.addOutputNeuron(0); // layer 1

        ConnectionGene firstEdge = b.addConnection(in, out);
        firstEdge.setWeight(1.0);
        Genome g = b.getGenome();

        AddNeuronMutation mut = new AddNeuronMutation(inv);

        // ── 1st split: in → out  ───────────────────────────────
        mut.replaceConnectionWithNeuron(g, firstEdge);

        // identify the new hidden node & its edge to the output
        NeuronGene h1 = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .findFirst().get();
        ConnectionGene edgeH1toOut = g.getConnection(h1.getInnovationId(), out.getInnovationId());

        // ── 2nd split: h1 → out (successor edge) ───────────────
        mut.replaceConnectionWithNeuron(g, edgeH1toOut);

        // collect layers
        List<Integer> hiddenLayers = g.getNeurons().stream()
                .filter(n -> n instanceof HiddenNeuronGene)
                .map(NeuronGene::getLayer)
                .sorted()
                .toList();

        assertEquals(List.of(1, 2), hiddenLayers,
                "hidden nodes should occupy layers 1 and 2");
        assertEquals(3, g.getNeuronById(out.getInnovationId()).getLayer(),
                "output should now be on layer 3");
    }

    @Test
    @DisplayName("Shifting affects outputs that sit deeper than the split")
    void shiftMovesDeeperOutputs() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in = b.addInputNeuron(0);   // L0
        OutputNeuronGene out = b.addOutputNeuron(0);  // L1
        HiddenNeuronGene hid = b.addHiddenNeuron(1);      // user-defined hidden at L2
        hid.setLayer(2);              // builder defaults to 1
        out.setLayer(1);              // addHiddenNeuron shifts outputs to 2

        b.addConnection(in, out).setWeight(1.0);
        b.addConnection(out, hid).setWeight(1.0);         // output → deeper hidden connection (feed-forward)

        Genome g = b.getGenome();

        // split input→output (gap must be created; both output & deeper hidden shift)
        new AddNeuronMutation(inv)
                .replaceConnectionWithNeuron(g, g.getConnection(in.getInnovationId(), out.getInnovationId()));

        assertEquals(2, g.getNeuronById(out.getInnovationId()).getLayer(), "output shifted from 1 → 2");
        assertEquals(3, g.getNeuronById(hid.getInnovationId()).getLayer(), "deep hidden shifted from 2 → 3");
    }

    @Test
    @DisplayName("AddConnectionMutation respects layer order")
    void addConnectionRespectsLayers() {
        InnovationService inv = new InnovationService();
        GenomeBuilder b = new GenomeBuilder(inv);

        // minimal 3-layer net (0->1->2)
        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid = b.addHiddenNeuron(1);
        OutputNeuronGene out = b.addOutputNeuron(0);

        b.addConnection(in, hid).setWeight(1.0);
        b.addConnection(hid, out).setWeight(1.0);
        Genome g = b.getGenome();

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
        GenomeBuilder b = new GenomeBuilder(inv);

        // input 0, output 1
        InputNeuronGene in = b.addInputNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in, out).setWeight(1.0);
        Genome g = b.getGenome();

        // split five times to deepen network
        AddNeuronMutation mut = new AddNeuronMutation(inv);
        for (int i = 0; i < 5; i++) {
            mut.replaceConnectionWithNeuron(g, g.getConnection(in.getInnovationId(), out.getInnovationId()));
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
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in = b.addInputNeuron(0);
        HiddenNeuronGene hid1 = b.addHiddenNeuron(1);
        HiddenNeuronGene hid2 = b.addHiddenNeuron(2);
        OutputNeuronGene out = b.addOutputNeuron(0);

        b.addConnection(in, hid1).setWeight(1.0);
        b.addConnection(hid1, hid2).setWeight(1.0);
        b.addConnection(hid2, out).setWeight(1.0);
        Genome g = b.getGenome();

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
            GenomeBuilder b = new GenomeBuilder(inv);

            InputNeuronGene[] in = {b.addInputNeuron(0), b.addInputNeuron(1)};
            OutputNeuronGene[] out = {b.addOutputNeuron(0), b.addOutputNeuron(1)};

            ConnectionGene[] c = {
                    b.addConnection(in[0], out[0]),
                    b.addConnection(in[1], out[1])
            };
            for (ConnectionGene conn : c) conn.setWeight(1.0);
            Genome g = b.getGenome();

            AddNeuronMutation mut = new AddNeuronMutation(inv);
            // perform two splits in specified order
            mut.replaceConnectionWithNeuron(g, c[order[0]]);
            mut.replaceConnectionWithNeuron(g, c[order[1]]);

            hiddenLayerResults.add(layers(g, HiddenNeuronGene.class));
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
            GenomeBuilder b = new GenomeBuilder(inv);

            // 2-input, 2-output starter
            InputNeuronGene in0 = b.addInputNeuron(0);
            InputNeuronGene in1 = b.addInputNeuron(1);
            OutputNeuronGene out0 = b.addOutputNeuron(0);
            OutputNeuronGene out1 = b.addOutputNeuron(1);

            ConnectionGene c0 = b.addConnection(in0, out0);
            c0.setWeight(1.0);
            ConnectionGene c1 = b.addConnection(in1, out1);
            c1.setWeight(1.0);

            Genome g = b.getGenome();

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
     * Collect layers of neurons of a given type, sorted ascending.
     */
    private static List<Integer> layers(Genome g, Class<? extends NeuronGene> t) {
        return g.getNeurons().stream()
                .filter(t::isInstance)
                .map(NeuronGene::getLayer)
                .sorted()
                .collect(Collectors.toList());
    }
}
