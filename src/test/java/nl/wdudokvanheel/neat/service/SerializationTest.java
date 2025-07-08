package nl.wdudokvanheel.neat.service;

import com.google.common.collect.Table;
import nl.wdudokvanheel.neural.neat.genome.*;
import nl.wdudokvanheel.neural.neat.mutation.AddNeuronMutation;
import nl.wdudokvanheel.neural.neat.service.GenomeBuilder;
import nl.wdudokvanheel.neural.neat.service.InnovationService;
import nl.wdudokvanheel.neural.neat.service.SerializationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SerializationTest {
    private Logger logger = LoggerFactory.getLogger(SerializationTest.class);

    private SerializationService serializationService = new SerializationService();

    private Genome getTestGenome(InnovationService innovationService) {
        GenomeBuilder builder = new GenomeBuilder(innovationService);
        InputNeuronGene[] inputs = builder.addInputNeurons(3);
        OutputNeuronGene[] outputs = builder.addOutputNeurons(1);

        builder.addHiddenNeuron(0);
        builder.addHiddenNeuron(1);

        builder.addConnection(inputs[0], outputs[0], 1.0);
        builder.addConnection(inputs[1], outputs[0], 3.0);
        builder.addConnection(inputs[2], outputs[0], 2.0);

        return builder.getGenome();
    }

    @Test
    @DisplayName("Connection genes with same properties should be equal")
    void equalsTestConnectionGene() {
        ConnectionGene a = new ConnectionGene(1, 2, 3, 3.0, false);
        ConnectionGene b = new ConnectionGene(1, 2, 3, 3.0, false);
        ConnectionGene c = new ConnectionGene(1, 2, 3, 3.0, true);
        ConnectionGene d = new ConnectionGene(1, 2, 3, 2.0, false);
        ConnectionGene e = new ConnectionGene(1, 3, 2, 3.0, false);
        ConnectionGene f = new ConnectionGene(0, 2, 3, 3.0, false);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
        assertNotEquals(a, f);
    }

    @Test
    @DisplayName("Neuron Genes with same properties should be equal")
    void equalsTestInputNeuronGenes() {
        InputNeuronGene a = new InputNeuronGene(1, 2);
        InputNeuronGene b = new InputNeuronGene(1, 2);

        InputNeuronGene c = new InputNeuronGene(0, 2);
        InputNeuronGene d = new InputNeuronGene(1, 0);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }

    @Test
    @DisplayName("Neuron Genes with same properties should be equal")
    void equalsTestOutputNeuronGenes() {
        OutputNeuronGene a = new OutputNeuronGene(1, 2);
        OutputNeuronGene b = new OutputNeuronGene(1, 2);

        OutputNeuronGene c = new OutputNeuronGene(0, 2);
        OutputNeuronGene d = new OutputNeuronGene(1, 0);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }

    @Test
    @DisplayName("Neuron Genes with same properties should be equal")
    void equalsTestHiddenNeuronGenes() {
        HiddenNeuronGene a = new HiddenNeuronGene(1, 2, 3);
        HiddenNeuronGene b = new HiddenNeuronGene(1, 2, 3);

        HiddenNeuronGene c = new HiddenNeuronGene(0, 2, 3);
        HiddenNeuronGene d = new HiddenNeuronGene(1, 0, 3);
        HiddenNeuronGene e = new HiddenNeuronGene(1, 2, 0);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
    }

    @Test
    @DisplayName("Neuron Genes with same properties should be equal")
    void equalsTestStaticHiddenNeuronGenes() {
        StaticHiddenNeuronGene a = new StaticHiddenNeuronGene(1, 2, 3);
        StaticHiddenNeuronGene b = new StaticHiddenNeuronGene(1, 2, 3);

        StaticHiddenNeuronGene c = new StaticHiddenNeuronGene(0, 2, 3);
        StaticHiddenNeuronGene d = new StaticHiddenNeuronGene(1, 0, 3);
        StaticHiddenNeuronGene e = new StaticHiddenNeuronGene(1, 2, 0);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
    }

    @Test
    @DisplayName("Genomes with same properties should be equal")
    void equalsTestGenomes() {
        InnovationService innovation = new InnovationService();
        Genome a = getTestGenome(innovation);
        Genome b = getTestGenome(innovation);

        Genome c = getTestGenome(innovation);
        c.getConnections().clear();

        Genome d = getTestGenome(innovation);
        d.getConnections().getFirst().setWeight(99);

        Genome e = getTestGenome(innovation);
        e.getNeurons().removeFirst();

        Genome f = getTestGenome(innovation);
        f.getNeurons().getFirst().setLayer(3);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
        assertNotEquals(a, f);
    }

    @Test
    @DisplayName("Serializing and deserializing should create the same genome")
    void serializeAndDeserialize() {
        InnovationService innovation = new InnovationService();
        Genome genome = getTestGenome(innovation);

        // Add two non-static hidden neuron
        new AddNeuronMutation(innovation).mutate(genome);
        new AddNeuronMutation(innovation).mutate(genome);

        String originalGenomeJson = serializationService.serialize(genome);
//        logger.info("{}", originalGenomeJson);

        Genome deserializedGenome = serializationService.deserialize(originalGenomeJson);
        assertEquals(genome, deserializedGenome);

        // Serialize again and see if the JSON is still the same
        String deserializedGenomeJson = serializationService.serialize(deserializedGenome);
        assertEquals(originalGenomeJson, deserializedGenomeJson);
    }

    @Test
    @DisplayName("importFromGenome should register neuron and connection IDs and set the innovation counter correctly")
    void importFromGenome() throws Exception {
        InnovationService innovation = new InnovationService();
        Genome genome = getTestGenome(innovation);
        // Add a few non-static hidden neuron
        new AddNeuronMutation(innovation).mutate(genome);
        new AddNeuronMutation(innovation).mutate(genome);
        new AddNeuronMutation(innovation).mutate(genome);

        // Extract private fields via reflection
        ConcurrentHashMap<Integer, Integer> inputIds = getField(innovation, "inputNeuronIds");
        ConcurrentHashMap<Integer, Integer> staticHiddenIds = getField(innovation, "staticHiddenNeuronIds");
        ConcurrentHashMap<Integer, Integer> hiddenIds = getField(innovation, "hiddenNeuronIds");
        ConcurrentHashMap<Integer, Integer> outputIds = getField(innovation, "outputNeuronIds");
        Table<Integer, Integer, Integer> connectionIds = getField(innovation, "connectionIds");
        AtomicInteger counter = getField(innovation, "innovationIdCounter");

        InnovationService innovationNew = new InnovationService();
        innovationNew.importFromGenome(genome);

        ConcurrentHashMap<Integer, Integer> inputIdsNew = getField(innovationNew, "inputNeuronIds");
        ConcurrentHashMap<Integer, Integer> staticHiddenIdsNew = getField(innovationNew, "staticHiddenNeuronIds");
        ConcurrentHashMap<Integer, Integer> hiddenIdsNew = getField(innovationNew, "hiddenNeuronIds");
        ConcurrentHashMap<Integer, Integer> outputIdsNew = getField(innovationNew, "outputNeuronIds");
        Table<Integer, Integer, Integer> connectionIdsNew = getField(innovationNew, "connectionIds");
        AtomicInteger counterNew = getField(innovationNew, "innovationIdCounter");

        assertEquals(inputIds, inputIdsNew);
        assertEquals(staticHiddenIds, staticHiddenIdsNew);
        assertEquals(hiddenIds, hiddenIdsNew);
        assertEquals(outputIds, outputIdsNew);
        assertEquals(connectionIds, connectionIdsNew);
        assertEquals(counter.get(), counterNew.get());
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
