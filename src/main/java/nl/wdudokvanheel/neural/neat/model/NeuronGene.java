package nl.wdudokvanheel.neural.neat.model;

public class NeuronGene {
    private NeuronGeneType type;
    private int innovationId;
    private int layer;

    public NeuronGene(NeuronGeneType type, int innovationId, int layer) {
        this.type = type;
        this.innovationId = innovationId;
        this.layer = layer;
    }

    public NeuronGene(NeuronGeneType type, int innovationId) {
        this(type, innovationId, 0);
    }

    public NeuronGeneType getType() {
        return type;
    }

    public int getInnovationId() {
        return innovationId;
    }

    public int getLayer() {
        return layer;
    }

    public NeuronGene setLayer(int layer) {
        this.layer = layer;
        return this;
    }

    @Override
    public NeuronGene clone() {
        return new NeuronGene(type, innovationId, layer);
    }

    @Override
    public String toString() {
        return "Neuron #" + getInnovationId() + " " + type + (layer > 0 ? " Layer " + layer : "");
    }
}
