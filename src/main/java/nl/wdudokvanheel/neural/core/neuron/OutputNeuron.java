package nl.wdudokvanheel.neural.core.neuron;

public class OutputNeuron extends Neuron{
	public OutputNeuron(int id){
		super(id, 1);
	}

	public OutputNeuron(int id, int layer){
		super(id, layer);
	}

	@Override
	public String toString(){
		return "Output #" + getId();
	}
}
