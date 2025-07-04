package nl.wdudokvanheel.neural.neat.model;

public class ConnectionGene {
    private int innovationId;
    private int source;
    private int target;
    private double weight;
    private boolean enabled;

    public ConnectionGene(int innovationId, int source, int target, double weight, boolean enabled) {
        this.innovationId = innovationId;
        this.source = source;
        this.target = target;
        this.weight = weight;
        this.enabled = enabled;
    }

    public ConnectionGene(int innovationId, int source, int target, double weight) {
        this(innovationId, source, target, weight, true);
    }

    public ConnectionGene(int innovationId, int source, int target) {
        this(innovationId, source, target, 1, true);
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public int getInnovationId() {
        return innovationId;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggleEnabled() {
        setEnabled(!enabled);
    }

    @Override
    public ConnectionGene clone() {
        return new ConnectionGene(innovationId, source, target, weight, enabled);
    }

    @Override
    public String toString() {
        return "Connection #" + innovationId + "(" + (enabled ? "Enabled" : "Disabled") + ") from neuron #" + source + " to #" + target + " with a weight of " + weight;
    }
}
