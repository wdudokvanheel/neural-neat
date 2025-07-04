package nl.wdudokvanheel.neural.neat.service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to generate and retrieve innovation ids for genome connections and nodes
 */
public class InnovationService {
    private final AtomicInteger innovation = new AtomicInteger();

    // maps are now concurrent so we don’t need explicit synchronisation
    private final ConcurrentHashMap<Integer, Integer> inputNeuronIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> outputNeuronIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> hiddenNeuronIds = new ConcurrentHashMap<>();

    private final Table<Integer, Integer, Integer> connectionIds = HashBasedTable.create();

    /**
     * Get the innovation id for a new hidden neuron
     *
     * @param connectionId The innovation id of the connection the neuron replaces
     */
    public int getNeuronInnovationId(int connectionId) {
        return hiddenNeuronIds.computeIfAbsent(connectionId,
                k -> nextInnovation());
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

    // ******* Helper & wrapper methods *******

    private int nextInnovation() {
        return innovation.incrementAndGet();           // single global counter
    }

    public int getConnectionInnovationId(NeuronGene source, NeuronGene target) {
        return getConnectionInnovationId(source.getInnovationId(), target.getInnovationId());
    }
}
