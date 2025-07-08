package nl.wdudokvanheel.neural.neat.genome;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OutputNeuronGene extends NeuronGene {
    @JsonCreator
    public OutputNeuronGene(
            @JsonProperty("innovationId") int innovationId,
            @JsonProperty("layer") int layer
    ) {
        super(innovationId, layer);
    }

    public OutputNeuronGene(int innovationId) {
        super(innovationId, 1);
    }

    @Override
    public OutputNeuronGene clone() {
        return new OutputNeuronGene(getInnovationId(), getLayer());
    }
}
