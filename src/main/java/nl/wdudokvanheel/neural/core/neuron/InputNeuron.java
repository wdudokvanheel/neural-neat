package nl.wdudokvanheel.neural.core.neuron;

public class InputNeuron extends Neuron {
	public InputNeuron(int id){
		super(id);
	}

	public void setValue(double value){
		this.value = value;
	}

	@Override
	public double getValue(){
		if(value == null)
			throw new IllegalStateException("Input not set for " + this);

		return value;
	}

	@Override
	public String toString(){
		return "Input #" + getId();
	}
}

