package nl.wdudokvanheel.neural.neat.genome;

public class OutputNeuronGene extends NeuronGene {
    public OutputNeuronGene(int innovationId, int layer) {
        super(innovationId, layer);
    }

    public OutputNeuronGene(int innovationId) {
        super(innovationId, 1);
    }

    @Override
    public OutputNeuronGene clone() {
        return new OutputNeuronGene(getInnovationId(), getLayer());
    }
}
