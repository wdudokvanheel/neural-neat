package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.genome.*;

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

    public HiddenNeuronGene addHiddenNeuron(int index) {
        int innovationId = innovationService.getStaticHiddenNeuronInnovationId(index);
        StaticHiddenNeuronGene hiddenNeuronGene = new StaticHiddenNeuronGene(index, innovationId, 1);
        genome.addNeuron(hiddenNeuronGene);

        // If a hidden neuron is added, make sure the outputs are moved to layer 2
        for (OutputNeuronGene outputNeuron : genome.getOutputNeurons()) {
            outputNeuron.setLayer(2);
        }
        return hiddenNeuronGene;
    }

    public InputNeuronGene[] addInputNeurons(int count) {
        InputNeuronGene[] inputs = new InputNeuronGene[count];
        for (int i = 0; i < count; i++) {
            inputs[i] = addInputNeuron(i);
        }

        return inputs;
    }

    public HiddenNeuronGene[] addHiddenNeurons(int count) {
        HiddenNeuronGene[] hidden = new HiddenNeuronGene[count];
        for (int i = 0; i < count; i++) {
            hidden[i] = addHiddenNeuron(i);
        }

        return hidden;
    }

    public OutputNeuronGene[] addOutputNeurons(int count) {
        OutputNeuronGene[] outputs = new OutputNeuronGene[count];
        for (int i = 0; i < count; i++) {
            outputs[i] = addOutputNeuron(i);
        }

        return outputs;
    }

    public ConnectionGene addConnection(NeuronGene source, NeuronGene target, double weight) {
        int sourceInnovationId = source.getInnovationId();
        int targetInnovationId = target.getInnovationId();
        int innovationId = innovationService.getConnectionInnovationId(sourceInnovationId, targetInnovationId);
        ConnectionGene connectionGene = new ConnectionGene(innovationId, sourceInnovationId, targetInnovationId, weight);
        genome.addConnection(connectionGene);
        return connectionGene;
    }

    public ConnectionGene addConnection(NeuronGene source, NeuronGene target) {
       return addConnection(source, target, 0);
    }

    public Genome getGenome() {
        return genome;
    }
}
