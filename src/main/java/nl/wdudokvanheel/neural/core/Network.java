package nl.wdudokvanheel.neural.core;

import nl.wdudokvanheel.neural.core.neuron.Connection;
import nl.wdudokvanheel.neural.core.neuron.InputNeuron;
import nl.wdudokvanheel.neural.core.neuron.Neuron;
import nl.wdudokvanheel.neural.core.neuron.OutputNeuron;
import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private Logger logger = LoggerFactory.getLogger(Network.class);

    public static int ID_COUNTER = 0;

    public int id;
    public List<InputNeuron> inputNeurons = new ArrayList<>();
    public List<Neuron> hiddenNeurons = new ArrayList<>();
    public List<OutputNeuron> outputNeurons = new ArrayList<>();

    public Network() {
        id = ID_COUNTER++;
    }

    public Network(Genome genome) {
        this();

        //Add all genes
        for (NeuronGene gene : genome.getNeurons()) {
            switch (gene.getType()) {
                case INPUT -> inputNeurons.add(new InputNeuron(gene.getInnovationId()));
                case HIDDEN -> hiddenNeurons.add(new Neuron(gene.getInnovationId(), gene.getLayer()));
                case OUTPUT -> outputNeurons.add(new OutputNeuron(gene.getInnovationId()));
            }
        }

        //Add all connections
        for (ConnectionGene gene : genome.getConnections()) {
            if (!gene.isEnabled())
                continue;
            Neuron source = getNeuronById(gene.getSource());
            Neuron target = getNeuronById(gene.getTarget());
            target.addConnection(source, gene.getWeight());
        }
    }

    public void setInput(double... values) {
        for (int i = 0; i < values.length; i++) {
            InputNeuron neuron = inputNeurons.get(i);
            if (neuron == null) {
                logger.warn("Input not found: {}", i);
                break;
            }
            neuron.setValue(values[i]);
        }
    }

    public List<Neuron> getAllNeurons() {
        ArrayList<Neuron> neurons = new ArrayList<>();
        neurons.addAll(inputNeurons);
        neurons.addAll(hiddenNeurons);
        neurons.addAll(outputNeurons);
        return neurons;
    }

    public List<Neuron> getAllNeuronsWithInputConnections() {
        ArrayList<Neuron> neurons = new ArrayList<>();
        neurons.addAll(hiddenNeurons);
        neurons.addAll(outputNeurons);
        return neurons;
    }

    public void resetNeuronValues() {
        getAllNeurons().forEach(neuron -> neuron.resetValue());
    }

    public InputNeuron createInputNeuron() {
        InputNeuron neuron = new InputNeuron(inputNeurons.size());
        inputNeurons.add(neuron);
        return neuron;
    }

    public void createInputNeurons(int neurons) {
        for (int i = 0; i < neurons; i++) {
            createInputNeuron();
        }
    }

    public Neuron createHiddenNeuron() {
        Neuron neuron = new Neuron(hiddenNeurons.size());
        hiddenNeurons.add(neuron);
        return neuron;
    }

    public void createHiddenNeurons(int neurons) {
        for (int i = 0; i < neurons; i++) {
            createHiddenNeuron();
        }
    }

    public OutputNeuron createOutputNeuron() {
        OutputNeuron neuron = new OutputNeuron(outputNeurons.size());
        outputNeurons.add(neuron);
        return neuron;
    }

    public void createOutputNeurons(int neurons) {
        for (int i = 0; i < neurons; i++) {
            createOutputNeuron();
        }
    }


    public double[] getOutputs() {
        double[] output = new double[outputNeurons.size()];
        for (int i = 0; i < output.length; i++) {
            output[i] = getOutput(i);
        }

        return output;
    }

    public Neuron getNeuronById(int id) {
        for (Neuron neuron : getAllNeurons()) {
            if (neuron.getId() == id) {
                return neuron;
            }
        }
        return null;
    }

    public double getOutput(int index) {
        return outputNeurons.get(index).getValue();
    }

    public double getOutput() {
        return getOutput(0);
    }

    public Neuron getInputNeuron(int index) {
        return inputNeurons.get(index);
    }

    public Neuron getOutputNeuron(int index) {
        return outputNeurons.get(index);
    }

    public Network clone() {
        Network network = new Network();
        network.createInputNeurons(this.inputNeurons.size());
        network.createHiddenNeurons(this.hiddenNeurons.size());
        network.createOutputNeurons(this.outputNeurons.size());

        List<Neuron> source = getAllNeurons();
        List<Neuron> target = network.getAllNeurons();

        for (int i = 0; i < source.size(); i++) {
            Neuron sourceNeuron = source.get(i);
            Neuron targetNeuron = target.get(i);

            for (int j = 0; j < sourceNeuron.inputs.size(); j++) {
                Connection sourceConnection = sourceNeuron.inputs.get(j);
                targetNeuron.addConnection(network.getNeuronByName(sourceConnection.source.toString()), sourceConnection.weight);
            }
        }

        return network;
    }

    public Neuron getNeuronByName(String name) {
        int id = Integer.parseInt(name.split("#")[1]);

        if (name.toLowerCase().startsWith("neuron"))
            return hiddenNeurons.get(id);

        if (name.toLowerCase().startsWith("input"))
            return inputNeurons.get(id);

        if (name.toLowerCase().startsWith("output"))
            return outputNeurons.get(id);

        return null;
    }

    public int getLayers() {
        int max = 0;
        for (Neuron neuron : hiddenNeurons) {
            if (neuron.layer > max) {
                max = neuron.layer;
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return "Network #" + id;
    }
}
