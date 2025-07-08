package nl.wdudokvanheel.neat.service;

import nl.wdudokvanheel.neural.neat.NeatConfiguration;
import nl.wdudokvanheel.neural.neat.NeatContext;
import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.*;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
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
        GenomeBuilder b = new GenomeBuilder(new InnovationService());
        InputNeuronGene in = b.addInputNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in, out).setWeight(0.1);
        return b.getGenome();
    }

    private static Genome staticRoomyGenome() {             // 2 inputs, 1 hidden, 1 output
        GenomeBuilder b = new GenomeBuilder(new InnovationService());
        InputNeuronGene in1 = b.addInputNeuron(0);
        InputNeuronGene in2 = b.addInputNeuron(1);
        HiddenNeuronGene hid = b.addHiddenNeuron(0);
        OutputNeuronGene out = b.addOutputNeuron(0);
        b.addConnection(in1, hid).setWeight(0.2);   // so AddNeuron can split
        b.addConnection(in2, hid).setWeight(0.2);
        b.addConnection(hid, out).setWeight(0.2);
        return b.getGenome();
    }

    private static Genome roomyGenome(InnovationService inv) {   // 2 inputs, 1 hidden, 1 output
        GenomeBuilder b = new GenomeBuilder(inv);

        InputNeuronGene in1 = b.addInputNeuron(0);
        InputNeuronGene in2 = b.addInputNeuron(1);
        HiddenNeuronGene hid = b.addHiddenNeuron(42);
        OutputNeuronGene out = b.addOutputNeuron(0);

        b.addConnection(in1, hid).setWeight(0.2);   // edges the Add-Neuron mutation can split
        b.addConnection(in2, hid).setWeight(0.2);
        b.addConnection(hid, out).setWeight(0.2);   // downstream edge
        return b.getGenome();
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
    @DisplayName("RandomWeightMutation changes weight distribution (σ≈SIGMA)")
    void randomWeightSigma() {
        RandomWeightMutation mut = new RandomWeightMutation(1.0);
        Genome g = staticRoomyGenome();

        List<Double> delta = new ArrayList<>();
        int connId = g.getConnections().getFirst().getInnovationId();
        for (int i = 0; i < 10_000; i++) {
            double before = g.getConnectionById(connId).getWeight();
            mut.mutate(g);
            double after = g.getConnectionById(connId).getWeight();
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
        ConnectionGene toSplit = g.getConnections().getFirst();

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