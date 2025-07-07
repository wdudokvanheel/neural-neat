package nl.wdudokvanheel.neural.neat.genome;

public class InputNeuronGene extends NeuronGene {
    public InputNeuronGene(int innovationId, int layer) {
        super(innovationId, layer);
    }

    public InputNeuronGene(int innovationId) {
        super(innovationId, 0);
    }

    @Override
    public InputNeuronGene clone() {
        return new InputNeuronGene(getInnovationId(), getLayer());
    }
}
