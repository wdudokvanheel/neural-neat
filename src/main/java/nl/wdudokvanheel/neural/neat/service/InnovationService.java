package nl.wdudokvanheel.neural.neat.service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.wdudokvanheel.neural.neat.genome.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to generate and retrieve innovation ids for genome connections and nodes
 */
public class InnovationService {
    private final AtomicInteger innovationIdCounter = new AtomicInteger();

    // maps are now concurrent so we don’t need explicit synchronisation
    private final ConcurrentHashMap<Integer, Integer> inputNeuronIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> outputNeuronIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> hiddenNeuronIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> staticHiddenNeuronIds = new ConcurrentHashMap<>();

    private final Table<Integer, Integer, Integer> connectionIds = HashBasedTable.create();

    /**
     * Get the innovation id for a new hidden neuron
     *
     * @param connectionId The innovation id of the connection the neuron replaces
     */
    public int getHiddenNeuronInnovationId(int connectionId) {
        return hiddenNeuronIds.computeIfAbsent(connectionId, k -> nextInnovation());
    }

    /**
     * Get the innovation id for a static hidden neuron (one that does not replace a connection, but is always present)
     *
     * @param index The innovation id of the index of the static hidden neuron
     */
    public int getStaticHiddenNeuronInnovationId(int index) {
        return staticHiddenNeuronIds.computeIfAbsent(index, k -> nextInnovation());
    }


    public boolean doesNeuronIdExist(int connectionId) {
        return hiddenNeuronIds.containsKey(connectionId);
    }

    /**
     * Get the innovation id for a new connection
     *
     * @param sourceNode Innovation id of the source node
     * @param targetNode Innovation id of the target node
     */
    public int getConnectionInnovationId(int src, int tgt) {
        synchronized (connectionIds) {                 // HashBasedTable isn’t thread-safe
            Integer id = connectionIds.get(src, tgt);
            if (id == null) {
                id = nextInnovation();
                connectionIds.put(src, tgt, id);
            }
            return id;
        }
    }

    /**
     * Get the innovation id of an input node
     *
     * @param index Index of the input node
     */
    public int getInputNodeInnovationId(int index) {
        return inputNeuronIds.computeIfAbsent(index, k -> nextInnovation());
    }

    /**
     * Get the innovation id of an output node
     *
     * @param index Index of the output node
     */
    public int getOutputNodeInnovationId(int index) {
        return outputNeuronIds.computeIfAbsent(index, k -> nextInnovation());
    }

    public void importFromGenome(Genome genome) {
        for (ConnectionGene conn : genome.getConnections()) {
            int id = conn.getInnovationId();
            bumpCounterTo(id);
            synchronized (connectionIds) {
                connectionIds.put(conn.getSource(), conn.getTarget(), id);
            }
        }

        InputNeuronGene[] inputs = genome.getInputNeurons().toArray(InputNeuronGene[]::new);
        HiddenNeuronGene[] staticHidden = genome.getHiddenNeurons().stream().filter(n -> n instanceof StaticHiddenNeuronGene).map(n -> (StaticHiddenNeuronGene) n).toArray(StaticHiddenNeuronGene[]::new);
        HiddenNeuronGene[] connectionHidden = genome.getHiddenNeurons().stream().filter(n -> !(n instanceof StaticHiddenNeuronGene)).toArray(HiddenNeuronGene[]::new);
        OutputNeuronGene[] outputs = genome.getOutputNeurons().toArray(OutputNeuronGene[]::new);

        for (int i = 0; i < inputs.length; i++) {
            inputNeuronIds.put(i, inputs[i].getInnovationId());
            bumpCounterTo(inputs[i].getInnovationId());
        }

        for (int i = 0; i < staticHidden.length; i++) {
            staticHiddenNeuronIds.put(i, staticHidden[i].getInnovationId());
            bumpCounterTo(staticHidden[i].getInnovationId());
        }

        for (int i = 0; i < connectionHidden.length; i++) {
            hiddenNeuronIds.put(connectionHidden[i].getConnectionId(), connectionHidden[i].getInnovationId());
            bumpCounterTo(connectionHidden[i].getInnovationId());
        }

        for (int i = 0; i < outputs.length; i++) {
            outputNeuronIds.put(i, outputs[i].getInnovationId());
            bumpCounterTo(outputs[i].getInnovationId());
        }
    }

    /**
     * Atomically advance the counter so it's at least `id`.
     */
    private void bumpCounterTo(int id) {
        while (true) {
            int current = innovationIdCounter.get();
            if (current >= id) {
                break;
            }
            if (innovationIdCounter.compareAndSet(current, id)) {
                break;
            }
        }
    }

    private int nextInnovation() {
        return innovationIdCounter.incrementAndGet();
    }

    public int getConnectionInnovationId(NeuronGene source, NeuronGene target) {
        return getConnectionInnovationId(source.getInnovationId(), target.getInnovationId());
    }
}
