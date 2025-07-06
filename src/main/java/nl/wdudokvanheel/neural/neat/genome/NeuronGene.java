package nl.wdudokvanheel.neural.neat.genome;

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
        this(type, innovationId, type == NeuronGeneType.OUTPUT ? 1 : 0);
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

    public void setLayer(int layer) {
        this.layer = layer;
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
