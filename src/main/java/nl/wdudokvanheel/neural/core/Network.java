package nl.wdudokvanheel.neural.core;

import nl.wdudokvanheel.neural.core.neuron.Connection;
import nl.wdudokvanheel.neural.core.neuron.InputNeuron;
import nl.wdudokvanheel.neural.core.neuron.Neuron;
import nl.wdudokvanheel.neural.core.neuron.OutputNeuron;
import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Network {
    public static int ID_COUNTER = 0;
    public final int id;

    private final Map<Integer, Neuron> neuronsById = new HashMap<>();

    public final List<InputNeuron> inputNeurons = new ArrayList<>();
    public final List<Neuron> hiddenNeurons = new ArrayList<>();
    public final List<OutputNeuron> outputNeurons = new ArrayList<>();

    public Network() {
        id = ID_COUNTER++;
    }

    public Network(Genome genome) {
        this();
        for (NeuronGene gene : genome.getNeurons()) {
            switch (gene.getType()) {
                case INPUT -> addNeuron(new InputNeuron(gene.getInnovationId()));
                case HIDDEN -> addNeuron(new Neuron(gene.getInnovationId(), gene.getLayer()));
                case OUTPUT -> addNeuron(new OutputNeuron(gene.getInnovationId()));
            }
        }

        //Add all connections
        for (ConnectionGene gene : genome.getConnections()) {
            if (!gene.isEnabled()) {
                continue;
            }
            Neuron source = getNeuronById(gene.getSource());
            Neuron target = getNeuronById(gene.getTarget());
            target.addConnection(source, gene.getWeight());
        }
    }

    public void setInput(double... values) {
        for (int i = 0; i < values.length && i < inputNeurons.size(); i++) {
            inputNeurons.get(i).setValue(values[i]);
        }
    }

    public List<Neuron> getAllNeurons() {
        ArrayList<Neuron> neurons = new ArrayList<>();
        neurons.addAll(inputNeurons);
        neurons.addAll(hiddenNeurons);
        neurons.addAll(outputNeurons);
        return neurons;
    }

    public double[] getOutputs() {
        double[] out = new double[outputNeurons.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = outputNeurons.get(i).getValue();
        }
        return out;
    }

    public double getOutput(int index) {
        return outputNeurons.get(index).getValue();
    }

    public double getOutput() {
        return getOutput(0);
    }

    public InputNeuron getInputNeuron(int index) {
        return inputNeurons.get(index);
    }

    public OutputNeuron getOutputNeuron(int index) {
        return outputNeurons.get(index);
    }

    public Network clone() {
        Network clone = new Network();
        // clone neurons
        for (InputNeuron n : inputNeurons) {
            clone.addNeuron(new InputNeuron(n.getId()));
        }
        for (Neuron n : hiddenNeurons) {
            clone.addNeuron(new Neuron(n.getId(), n.layer));
        }
        for (OutputNeuron n : outputNeurons) {
            clone.addNeuron(new OutputNeuron(n.getId()));
        }

        // clone connections
        for (Neuron n : neuronsById.values()) {
            for (Connection c : n.inputs) {
                Neuron src = clone.getNeuronById(c.source.getId());
                Neuron tgt = clone.getNeuronById(n.getId());
                tgt.addConnection(src, c.weight);
            }
        }
        return clone;
    }

    public Neuron getNeuronById(int id) {
        return neuronsById.get(id);
    }

    public Neuron getNeuronByName(String name) {
        int id = Integer.parseInt(name.split("#")[1].trim());
        return getNeuronById(id);
    }

    public void resetNeuronValues() {
        neuronsById.values().forEach(Neuron::resetValue);
    }

    public int getLayers() {
        return hiddenNeurons.stream().mapToInt(n -> n.layer).max().orElse(0);
    }

    @Override
    public String toString() {
        return "Network #" + id;
    }

    private void addNeuron(InputNeuron n) {
        inputNeurons.add(n);
        neuronsById.put(n.getId(), n);
    }

    private void addNeuron(OutputNeuron n) {
        outputNeurons.add(n);
        neuronsById.put(n.getId(), n);
    }

    private void addNeuron(Neuron n) {
        hiddenNeurons.add(n);
        neuronsById.put(n.getId(), n);
    }
}
