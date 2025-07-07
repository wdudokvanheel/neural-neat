package nl.wdudokvanheel.neural.neat.genome;

public class HiddenNeuronGene extends NeuronGene {
    public HiddenNeuronGene(int innovationId, int layer) {
        super(innovationId, layer);
    }

    public HiddenNeuronGene(int innovationId) {
        super(innovationId, 0);
    }

    @Override
    public HiddenNeuronGene clone() {
        return new HiddenNeuronGene(getInnovationId(), getLayer());
    }
}
