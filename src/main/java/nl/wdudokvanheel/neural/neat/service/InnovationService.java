package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.HashMap;

/**
 * Service to generate and retrieve innovation ids for genome connections and nodes
 */
public class InnovationService {
    private HashMap<Integer, Integer> inputNeuronIds = new HashMap<>();
    private HashMap<Integer, Integer> outputNeuronIds = new HashMap<>();
    private HashMap<Integer, Integer> hiddenNeuronId = new HashMap<>();

    private Table<Integer, Integer, Integer> connectionIds = HashBasedTable.create();

    private int neuronCounter = 0;
    private int connectionCounter = 0;

    /**
     * Get the innovation id for a new hidden neuron
     *
     * @param connectionId The innovation id of the connection the neuron replaces
     */
    public int getNeuronInnovationId(int connectionId) {
        Integer id = hiddenNeuronId.get(connectionId);
        if (id != null) {
            return id;
        }

        id = getNextNeuronInnovationId();
        hiddenNeuronId.put(connectionId, id);
        return id;
    }


    public boolean doesNeuronIdExist(int connectionId) {
        return hiddenNeuronId.get(connectionId) != null;
    }

    /**
     * Get the innovation id for a new connection
     *
     * @param sourceNode Innovation id of the source node
     * @param targetNode Innovation id of the target node
     */
    public int getConnectionInnovationId(int sourceNode, int targetNode) {
        Integer id = connectionIds.get(sourceNode, targetNode);
        if (id != null) {
            return id;
        }

        id = getNextConnectionInnovationId();
        connectionIds.put(sourceNode, targetNode, id);
        return id;
    }

    /**
     * Get the innovation id of an input node
     *
     * @param index Index of the input node
     */
    public int getInputNodeInnovationId(int index) {
        Integer id = inputNeuronIds.get(index);
        if (id != null) {
            return id;
        }

        id = getNextNeuronInnovationId();
        inputNeuronIds.put(index, id);
        return id;
    }

    /**
     * Get the innovation id of an output node
     *
     * @param index Index of the output node
     */
    public int getOutputNodeInnovationId(int index) {
        Integer id = outputNeuronIds.get(index);
        if (id != null) {
            return id;
        }

        id = getNextNeuronInnovationId();
        outputNeuronIds.put(index, id);
        return id;
    }

    private int getNextNeuronInnovationId() {
        return ++neuronCounter;
    }

    private int getNextConnectionInnovationId() {
        return ++connectionCounter;
    }

    // ******* Helper & wrapper methods *******

    public int getConnectionInnovationId(NeuronGene source, NeuronGene target) {
        return getConnectionInnovationId(source.getInnovationId(), target.getInnovationId());
    }
}
