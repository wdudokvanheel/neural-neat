package nl.wdudokvanheel.neural.core.neuron;

/**
 * @Author Wesley Dudok van Heel
 */
public class Connection{
	public Neuron source;
	public Neuron target;

	public double weight = 0;

	public Connection(Neuron source, Neuron target){
		this.source = source;
		this.target = target;
	}
}
