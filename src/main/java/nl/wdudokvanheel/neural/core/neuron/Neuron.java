package nl.wdudokvanheel.neural.core.neuron;

import nl.wdudokvanheel.neural.core.function.ActivationFunction;
import nl.wdudokvanheel.neural.core.function.SigmoidFunction;

import java.util.ArrayList;
import java.util.List;

public class Neuron{
	private int id;
	public ActivationFunction function;
	public List<Connection> inputs = new ArrayList<>();
	protected Double value = null;
	public int layer;

	public Neuron(int id, int layer, ActivationFunction function){
		this.id = id;
		this.layer = layer;
		this.function = function;
	}

	public Neuron(int id, int layer){
		this(id, layer, new SigmoidFunction());
	}

	public Neuron(int id){
		this(id, 0, new SigmoidFunction());
	}

	public double getValue(){
		if(value != null)
			return value;

		double total = 0;

		for(Connection input : inputs){
			double val = input.source.getValue();
			val *= input.weight;
			total += val;
		}

		value = function.perform(total);
		return value;
	}

	public Connection addConnection(Neuron source, double weight){
		Connection connection = new Connection(source, this);
		connection.weight = weight;
		inputs.add(connection);
		return connection;
	}

	public void addConnection(Neuron input){
		addConnection(input, 0);
	}

	public void resetValue(){
		value = null;
	}

	public int getId(){
		return id;
	}

	@Override
	public String toString(){
		return "Neuron #" + getId();
	}
}
