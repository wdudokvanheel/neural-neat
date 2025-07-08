package nl.wdudokvanheel.neural.neat.genome;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A static hidden neuron is the same as a regular hidden neuron, but is always present; it does not replace a connection
 * and is available from the (evolutionary) start of the genome.
 */

@JsonIgnoreProperties({ "connectionId"})
public class StaticHiddenNeuronGene extends HiddenNeuronGene {
    @JsonCreator
    public StaticHiddenNeuronGene(
            @JsonProperty("index") int index,
            @JsonProperty("innovationId") int innovationId,
            @JsonProperty("layer") int layer
    ) {
        super(index, innovationId, layer);
    }

    public int getIndex() {
        return getConnectionId();
    }
}
