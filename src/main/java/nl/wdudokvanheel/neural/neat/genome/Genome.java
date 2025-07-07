package nl.wdudokvanheel.neural.neat.genome;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Genome {
    private List<NeuronGene> neurons = new ArrayList<>();
    private List<ConnectionGene> connections = new ArrayList<>();

    public void addNeuron(NeuronGene neuron) {
        neurons.add(neuron);
    }

    public void addNeurons(NeuronGene... neurons) {
        for (NeuronGene neuron : neurons)
            addNeuron(neuron);
    }

    public void addConnection(ConnectionGene connection) {
        connections.add(connection);
    }

    public void addConnections(ConnectionGene... connections) {
        for (ConnectionGene connection : connections) {
            addConnection(connection);
        }
    }

    public List<NeuronGene> getNeurons() {
        return neurons;
    }

    public List<InputNeuronGene> getInputNeurons() {
        return neurons
                .stream()
                .filter(neuron -> neuron instanceof InputNeuronGene)
                .map(neuron -> (InputNeuronGene) neuron)
                .toList();
    }

    public List<HiddenNeuronGene> getHiddenNeurons() {
        return neurons
                .stream()
                .filter(neuron -> neuron instanceof HiddenNeuronGene)
                .map(neuron -> (HiddenNeuronGene) neuron)
                .toList();
    }

    public List<OutputNeuronGene> getOutputNeurons() {
        return neurons
                .stream()
                .filter(neuron -> neuron instanceof OutputNeuronGene)
                .map(neuron -> (OutputNeuronGene) neuron)
                .toList();
    }

    public List<ConnectionGene> getConnections() {
        return connections;
    }

    public List<ConnectionGene> getActiveConnections() {
        return connections.stream().filter(c -> c.isEnabled()).collect(Collectors.toList());
    }

    public NeuronGene getNeuronById(int innovationId) {
        for (NeuronGene neuron : neurons) {
            if (neuron.getInnovationId() == innovationId)
                return neuron;
        }

        return null;
    }

    public boolean hasNeuron(int id) {
        return getNeuronById(id) != null;
    }

    public boolean hasNeuron(NeuronGene neuron) {
        return getNeuronById(neuron.getInnovationId()) != null;
    }

    public ConnectionGene getConnectionById(int innovationId) {
        for (ConnectionGene connection : connections) {
            if (connection.getInnovationId() == innovationId) {
                return connection;
            }
        }
        return null;
    }

    public ConnectionGene getConnection(int source, int target) {
        for (ConnectionGene connection : connections) {
            if (connection.getSource() == source && connection.getTarget() == target) {
                return connection;
            }
        }
        return null;
    }

    public boolean hasConnection(int innovationId) {
        return getConnectionById(innovationId) != null;
    }

    public boolean hasConnection(int source, int target) {
        return getConnection(source, target) != null;
    }

    public boolean hasConnection(NeuronGene source, NeuronGene target) {
        return hasConnection(source.getInnovationId(), target.getInnovationId());
    }

    public boolean hasConnection(ConnectionGene connection) {
        return hasConnection(connection.getInnovationId());
    }

    public Genome clone() {
        Genome clone = new Genome();
        for (NeuronGene neuron : neurons) {
            clone.addNeuron(neuron.clone());
        }

        for (ConnectionGene connection : connections) {
            clone.addConnection(connection.clone());
        }
        return clone;
    }
}
