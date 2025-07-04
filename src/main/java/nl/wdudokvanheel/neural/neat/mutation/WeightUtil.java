package nl.wdudokvanheel.neural.neat.mutation;

public final class WeightUtil {
    private static final double LIMIT = 5.0;

    public static double clamp(double w) {
        if (w > LIMIT) {
            return LIMIT;
        }
        return Math.max(w, -LIMIT);
    }
}