package nl.wdudokvanheel.neural.network.function;

public class SigmoidFunction implements ActivationFunction{
	@Override
	public double perform(double value){
		return 1.0 / (1.0 + Math.exp(-4.9 * value));
	}
}
