package nl.wdudokvanheel.neural.util;

import nl.wdudokvanheel.neural.neat.CreatureInterface;
import nl.wdudokvanheel.neural.neat.Species;
import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;
import nl.wdudokvanheel.neural.neat.genome.NeuronGene;
import nl.wdudokvanheel.neural.network.Network;
import nl.wdudokvanheel.neural.network.neuron.Connection;
import nl.wdudokvanheel.neural.network.neuron.Neuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

public class Print {
    private static Logger logger = LoggerFactory.getLogger(Print.class);
    private static DecimalFormat df;

    static {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');

        df = new DecimalFormat("#.########", otherSymbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static void genome(Genome genome) {
        logger.debug("Genome - Neurons: " + genome.getNeurons().size() + " - Connections: " + genome.getConnections().size());

        logger.debug("Neurons:");
        for (NeuronGene neuron : genome.getNeurons()) {
            logger.debug("\t{}", neuron);
        }

        if (!genome.getConnections().isEmpty()) {
            logger.debug("Connections:");
        }

        for (ConnectionGene connection : genome.getConnections()) {
            NeuronGene source = genome.getNeuronById(connection.getSource());
            NeuronGene destination = genome.getNeuronById(connection.getTarget());
            logger.debug("\tConnection #" + connection.getInnovationId() + " (" + (connection.isEnabled() ? "Enabled" : "Disabled") + ") " + source + " -> " + destination + " with weight of " + connection.getWeight());
        }
    }

    public static <Creature extends CreatureInterface<Creature>> void pop(List<Creature> creatures) {
        for (int i = 0; i < creatures.size(); i++) {
            Creature creature = creatures.get(i);
            logger.debug("Creature #{} - Fitness: {}", i, creature.getFitness());
            genome(creature.getGenome());
            logger.debug("==============");
        }
    }

    public static <Creature extends CreatureInterface<Creature>> void species(List<Species<Creature>> species) {
        for (Species<Creature> iter : species) {
            logger.debug("Species {} with {} creatures", iter, iter.getCreatures().size());
        }
    }

    public static void network(Network network) {
        logger.debug("Network #{}", network.id);

        logger.debug("\tInput neurons");
        for (Neuron neuron : network.inputNeurons) {
            logger.debug("\t\t{}", neuron);
        }
        logger.debug("");
        logger.debug("\tHidden neurons");
        for (Neuron neuron : network.hiddenNeurons) {
            logger.debug("\t\t{} L{}", neuron, neuron.layer);
            for (Connection input : neuron.inputs) {
                logger.debug("\t\t\t{} -> | Weight: {}", input.source, input.weight);
            }
        }
        logger.debug("");
        logger.debug("\tOutput neurons");
        for (Neuron neuron : network.outputNeurons) {
            logger.debug("\t\t{}", neuron);
            for (Connection input : neuron.inputs) {
                logger.debug("\t\t\t{}  -> | Weight: {}", input.source, input.weight);
            }
        }
    }

    public static String format(double value) {
        return df.format(value);
    }
}
