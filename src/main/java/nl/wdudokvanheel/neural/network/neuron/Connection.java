package nl.wdudokvanheel.neural.network.neuron;

public class Connection{
	public Neuron source;
	public Neuron target;

	public double weight = 0;

	public Connection(Neuron source, Neuron target){
		this.source = source;
		this.target = target;
	}
}
