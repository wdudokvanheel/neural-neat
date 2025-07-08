package nl.wdudokvanheel.neural.neat.genome;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HiddenNeuronGene extends NeuronGene {
    private int connectionId = 0;

    @JsonCreator
    public HiddenNeuronGene(
            @JsonProperty("connectionId") int connectionId,
            @JsonProperty("innovationId") int innovationId,
            @JsonProperty("layer") int layer
    ) {
        super(innovationId, layer);
        this.connectionId = connectionId;
    }

    public int getConnectionId() {
        return connectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HiddenNeuronGene other)) {
            return false;
        }

        return other.connectionId == connectionId && super.equals(o);
    }

    @Override
    public HiddenNeuronGene clone() {
        return new HiddenNeuronGene(connectionId, getInnovationId(), getLayer());
    }
}
