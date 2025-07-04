package nl.wdudokvanheel.neural;

public class DoubleRange {
    public double min;
    public double max;

    public DoubleRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public DoubleRange(double max) {
        this(0, max);
    }
}
