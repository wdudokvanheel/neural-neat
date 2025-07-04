package nl.wdudokvanheel.neural.core.function;

public class SigmoidFunction implements ActivationFunction{
	@Override
	public double perform(double value){
//		return 1 / (1 + Math.exp(-value));
		return 1.0 / (1.0 + Math.exp(-4.9 * value));
	}
}
