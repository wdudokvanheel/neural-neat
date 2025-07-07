package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.genome.*;

import java.util.ArrayList;
import java.util.List;

public class GenomeBuilder {
    private final InnovationService innovationService;
    private final Genome genome;

    public GenomeBuilder(InnovationService innovationService) {
        this.innovationService = innovationService;
        this.genome = new Genome();
    }

    public InputNeuronGene addInputNeuron(int index) {
        int innovationId = innovationService.getInputNodeInnovationId(index);
        InputNeuronGene inputNeuronGene = new InputNeuronGene(innovationId, 0);
        genome.addNeuron(inputNeuronGene);
        return inputNeuronGene;
    }

    public OutputNeuronGene addOutputNeuron(int index) {
        int outputLayer = genome.getHiddenNeurons().isEmpty() ? 1 : 2;

        int innovationId = innovationService.getOutputNodeInnovationId(index);
        OutputNeuronGene outputNeuronGene = new OutputNeuronGene(innovationId, outputLayer);
        genome.addNeuron(outputNeuronGene);
        return outputNeuronGene;
    }

    public List<InputNeuronGene> addInputNeurons(int count) {
        List<InputNeuronGene> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            inputs.add(addInputNeuron(i));
        }

        return inputs;
    }

    public List<OutputNeuronGene> addOutputNeurons(int count) {
        List<OutputNeuronGene> outputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            outputs.add(addOutputNeuron(i));
        }

        return outputs;
    }

    public HiddenNeuronGene addHiddenNeuron(int index) {
        int innovationId = innovationService.getNeuronInnovationId(index);
        HiddenNeuronGene hiddenNeuronGene = new HiddenNeuronGene(innovationId, 1);
        genome.addNeuron(hiddenNeuronGene);

        // If a hidden neuron is added, make sure the outputs are moved to layer 2
        for (OutputNeuronGene outputNeuron : genome.getOutputNeurons()) {
            outputNeuron.setLayer(2);
        }
        return hiddenNeuronGene;
    }

    public List<HiddenNeuronGene> addHiddenNeurons(int count) {
        List<HiddenNeuronGene> hidden = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            hidden.add(addHiddenNeuron(i));
        }

        return hidden;
    }

    public ConnectionGene addConnection(NeuronGene source, NeuronGene target) {
        int sourceInnovationId = source.getInnovationId();
        int targetInnovationId = target.getInnovationId();
        int innovationId = innovationService.getConnectionInnovationId(sourceInnovationId, targetInnovationId);
        ConnectionGene connectionGene = new ConnectionGene(innovationId, sourceInnovationId, targetInnovationId, 0);
        genome.addConnection(connectionGene);
        return connectionGene;
    }

    public Genome getGenome() {
        return genome;
    }
}
