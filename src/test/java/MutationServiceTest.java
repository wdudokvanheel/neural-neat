import nl.wdudokvanheel.neural.neat.model.*;
import nl.wdudokvanheel.neural.neat.mutation.*;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.MutationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MutationService regression-suite.
 */
class MutationServiceTest {

    private static Genome minimalGenome() {
        Genome g = new Genome();
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, 1, 0),
                new NeuronGene(NeuronGeneType.OUTPUT, 2, 1)
        );
        g.addConnection(new ConnectionGene(1, 1, 2, 0.1, true));
        return g;
    }

    private static Genome staticRoomyGenome() {             // 2 inputs, 1 hidden, 1 output
        Genome g = new Genome();
        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, 1, 0),
                new NeuronGene(NeuronGeneType.INPUT, 2, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, 3, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, 4, 2)
        );
        g.addConnections(
                new ConnectionGene(1, 1, 3, 0.2, true),   // so AddNeuron can split
                new ConnectionGene(2, 2, 3, 0.2, true),
                new ConnectionGene(3, 3, 4, 0.2, true)
        );
        return g;
    }

    private static Genome roomyGenome(InnovationService inv) {   // 2 inputs, 1 hidden, 1 output
        Genome g = new Genome();

        int in1 = inv.getInputNodeInnovationId(0);          // layer 0
        int in2 = inv.getInputNodeInnovationId(1);          // layer 0
        int hid = inv.getNeuronInnovationId(42);            // layer 1 (arbitrary “split-key”)
        int out = inv.getOutputNodeInnovationId(0);         // layer 2

        g.addNeurons(
                new NeuronGene(NeuronGeneType.INPUT, in1, 0),
                new NeuronGene(NeuronGeneType.INPUT, in2, 0),
                new NeuronGene(NeuronGeneType.HIDDEN, hid, 1),
                new NeuronGene(NeuronGeneType.OUTPUT, out, 2)
        );

        g.addConnections(
                // edges the Add-Neuron mutation can split
                new ConnectionGene(inv.getConnectionInnovationId(in1, hid), in1, hid, 0.2, true),
                new ConnectionGene(inv.getConnectionInnovationId(in2, hid), in2, hid, 0.2, true),
                // downstream edge
                new ConnectionGene(inv.getConnectionInnovationId(hid, out), hid, out, 0.2, true)
        );
        return g;
    }

    private static NeatConfiguration config(boolean multiple) {
        NeatConfiguration c = new NeatConfiguration();

        c.multipleMutationsPerGenome = multiple;

        // use exaggerated probabilities for easy counting
        c.mutateAddConnectionProbability = 0.30;
        c.mutateAddNeuronProbability = 0.20;
        c.mutateToggleConnectionProbability = 0.10;
        c.mutateWeightProbability = 0.40;

        return c;
    }

    private static void injectRandom(MutationService mut, long seed) throws Exception {
        Random fixed = new Random(seed);

        // 1) MutationService.random
        Field rndFld = MutationService.class.getDeclaredField("random");
        rndFld.setAccessible(true);
        rndFld.set(mut, fixed);

        // 2) Random inside each Mutation (field 'random' inherited from AbstractMutation)
        Field mapFld = MutationService.class.getDeclaredField("mutations");
        mapFld.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Mutation, Double> m = (Map<Mutation, Double>) mapFld.get(mut);

        for (Mutation op : m.keySet()) {
            Field r = op.getClass().getSuperclass().getDeclaredField("random");
            r.setAccessible(true);
            r.set(op, fixed);

            // WeightMutation owns two inner mutation objects; patch those too
            if (op instanceof WeightMutation wm) {
                Field rwFld = WeightMutation.class.getDeclaredField("randomWeightMutation");
                Field swFld = WeightMutation.class.getDeclaredField("shiftWeightMutation");
                rwFld.setAccessible(true);
                swFld.setAccessible(true);
                AbstractMutation rw = (AbstractMutation) rwFld.get(wm);
                AbstractMutation sw = (AbstractMutation) swFld.get(wm);

                Field rwRand = rw.getClass().getSuperclass().getDeclaredField("random");
                Field swRand = sw.getClass().getSuperclass().getDeclaredField("random");
                rwRand.setAccessible(true);
                swRand.setAccessible(true);
                rwRand.set(rw, fixed);
                swRand.set(sw, fixed);
            }
        }
    }

    @Test
    @DisplayName("Exactly one mutation fires when multipleMutationsPerGenome = false")
    void singleMutationMode() {
        NeatContext ctx = new NeatContext(null, config(false));
        MutationService mut = ctx.mutationService;

        Genome g = minimalGenome();
        int neuronsBefore = g.getNeurons().size();
        int connsBefore = g.getConnections().size();

        mut.mutateGenome(g);

        int neuronsAfter = g.getNeurons().size();
        int connsAfter = g.getConnections().size();

        int neur = neuronsAfter - neuronsBefore;
        int conn = connsAfter - connsBefore;

    /* legal outcomes for ONE mutation:
       weight / toggle          → neur=0 conn=0
       add-connection           → neur=0 conn=1
       add-neuron               → neur=1 conn=2
    */
        boolean ok =
                (neur == 0 && conn == 0) ||
                        (neur == 0 && conn == 1) ||
                        (neur == 1 && conn == 2);

        assertTrue(ok, "More than one mutation seems to have been applied "
                + "(neur=" + neur + ", conn=" + conn + ")");
    }

    @Test
    @DisplayName("WeightUtil.clamp keeps weights within ±LIMIT")
    void clampRange() {
        Random rnd = new Random();
        for (int i = 0; i < 10_000; i++) {
            double w = rnd.nextGaussian() * 50;          // extreme random weight
            double clamped = WeightUtil.clamp(w);
            assertTrue(clamped <= 5.0 && clamped >= -5.0,
                    "clamp failed for " + w);
        }
    }

    @Test
    @DisplayName("ShiftWeightMutation: weight always clamped")
    void shiftWeightClamp() {
        InnovationService inv = new InnovationService();
        Genome g = staticRoomyGenome();
        // set a near-limit weight so overflow is likely
        g.getConnectionById(1).setWeight(4.9);

        ShiftWeightMutation mut = new ShiftWeightMutation(100); // huge perturbation

        for (int i = 0; i < 1_000; i++) {
            mut.mutate(g);
            g.getConnections().forEach(
                    c -> assertTrue(Math.abs(c.getWeight()) <= 5.0,
                            "weight out of clamp range: " + c.getWeight()));
        }
    }

    @Test
    @DisplayName("RandomWeightMutation changes weight distribution (σ≈SIGMA)")
    void randomWeightSigma() {
        RandomWeightMutation mut = new RandomWeightMutation();
        InnovationService inv = new InnovationService();
        Genome g = staticRoomyGenome();

        List<Double> delta = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            double before = g.getConnectionById(1).getWeight();
            mut.mutate(g);
            double after = g.getConnectionById(1).getWeight();
            delta.add(after - before);
        }
        double variance = delta.stream().mapToDouble(d -> d * d).sum() / delta.size();
        double sigma = Math.sqrt(variance);
        assertTrue(sigma > 0.3 && sigma < 0.7, "σ out of expected range: " + sigma);
    }

    @Test
    @DisplayName("AddConnectionMutation inserts only forward unique edges")
    void addConnectionLegality() {
        InnovationService inv = new InnovationService();
        AddConnectionMutation mut = new AddConnectionMutation(inv);

        for (int run = 0; run < 500; run++) {
            Genome g = staticRoomyGenome();
            mut.mutate(g);

            Set<String> seen = new HashSet<>();
            for (ConnectionGene c : g.getConnections()) {
                // uniqueness
                String key = c.getSource() + "->" + c.getTarget();
                assertTrue(seen.add(key), "duplicate connection " + key);
                // legality: source.layer < target.layer
                int srcL = g.getNeuronById(c.getSource()).getLayer();
                int tgtL = g.getNeuronById(c.getTarget()).getLayer();
                assertTrue(srcL < tgtL,
                        "illegal direction " + key + " (" + srcL + ">=" + tgtL + ")");
            }
        }
    }

    @Test
    @DisplayName("AddNeuronMutation splits a connection only once")
    void addNeuronIdempotent() {
        InnovationService inv = new InnovationService();
        Genome g = staticRoomyGenome();
        ConnectionGene toSplit = g.getConnectionById(1);

        AddNeuronMutation mut = new AddNeuronMutation(inv);
        mut.replaceConnectionWithNeuron(g, toSplit);  // first time
        int neuronCount1 = g.getNeurons().size();

        mut.replaceConnectionWithNeuron(g, toSplit);  // second time
        int neuronCount2 = g.getNeurons().size();

        assertEquals(neuronCount1, neuronCount2,
                "connection replaced twice, duplicate neuron created");
    }

    @Test
    @DisplayName("multipleMutationsPerGenome=true allows all mutation kinds to take effect (deterministic seed)")
    void allMutationsPossible() throws Exception {

        // --- configuration: every mutation attempted ------------------
        NeatConfiguration cfg = new NeatConfiguration();
        cfg.multipleMutationsPerGenome = true;
        cfg.mutateAddConnectionProbability = 1.0;
        cfg.mutateAddNeuronProbability = 1.0;
        cfg.mutateToggleConnectionProbability = 1.0;
        cfg.mutateWeightProbability = 1.0;

        InnovationService inv = new InnovationService();
        MutationService mut = new MutationService(cfg, inv);

        // inject fixed Random so outcomes are repeatable
        injectRandom(mut, 42L);

        Genome g = roomyGenome(inv);   // 4 neurons, 3 connections

        // --- act -------------------------------------------------------
        mut.mutateGenome(g);

        // --- assert: every mutation left a footprint ------------------

        // Add-Neuron: +1 neuron, +2 connections (one disabled)
        assertTrue(g.getNeurons().size() >= 5, "neuron not added");
        assertTrue(g.getConnections().size() >= 6, "connections not added");

        // Toggle-Connection: at least one disabled edge present
        long disabled = g.getConnections().stream()
                .filter(c -> !c.isEnabled())
                .count();
        assertTrue(disabled >= 1, "no connection toggled off");

        // Weight-mutation: some weight differs from the original 0.2
        boolean weightChanged = g.getConnections().stream()
                .anyMatch(c -> Math.abs(c.getWeight() - 0.2) > 1e-6);
        assertTrue(weightChanged, "weights unchanged");
    }
}