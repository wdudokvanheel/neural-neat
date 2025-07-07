package nl.wdudokvanheel.neural.neat.genome;

/**
 * Base class for all neuron genes.
 */
public abstract class NeuronGene {
    private final int innovationId;
    private int layer;

    protected NeuronGene(int innovationId, int layer) {
        this.innovationId = innovationId;
        this.layer = layer;
    }

    protected NeuronGene(int innovationId) {
        this(innovationId, 0);
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
    public abstract NeuronGene clone();

    @Override
    public String toString() {
        String type = getClass().getSimpleName().replace("NeuronGene", "");
        return "Neuron #" + getInnovationId() + " " + type + (layer > 0 ? " Layer " + layer : "");
    }
}
