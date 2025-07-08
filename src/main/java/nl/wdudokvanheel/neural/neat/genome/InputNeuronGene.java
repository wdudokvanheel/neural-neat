package nl.wdudokvanheel.neural.neat.genome;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InputNeuronGene extends NeuronGene {
    @JsonCreator
    public InputNeuronGene(
            @JsonProperty("innovationId") int innovationId,
            @JsonProperty("layer") int layer
    ) {
        super(innovationId, layer);
    }

    public InputNeuronGene(int innovationId) {
        super(innovationId, 0);
    }

    @Override
    public InputNeuronGene clone() {
        return new InputNeuronGene(getInnovationId(), getLayer());
    }
}
