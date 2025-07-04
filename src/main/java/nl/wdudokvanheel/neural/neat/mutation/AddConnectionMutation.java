package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.service.InnovationService;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AddConnectionMutation extends AbstractMutation {
    private InnovationService innovationService;

    public AddConnectionMutation(InnovationService innovationService) {
        this.innovationService = innovationService;
    }

    @Override
    public void mutate(Genome genome) {
        List<NeuronGene> sources = getAvailableConnectionSources(genome);

        if (sources.size() == 0) {
            return;
        }

        //Keep trying until there are no more possible source neurons to create a connection from
        while (sources.size() > 0) {
            NeuronGene source = getRandomElement(sources);

            //Get a list of possible connection targets for this neuron
            List<NeuronGene> targets = getPossibleTargetsForNewConnection(genome, source);

            //No targets found for this source, remove it from the list of possible sources and try again
            if (targets.size() == 0) {
                sources.remove(source);
                continue;
            }

            //Get a random target to connect
            NeuronGene target = getRandomElement(targets);
            int id = innovationService.getConnectionInnovationId(source, target);

            //Create a new connection from the source to the target with a random weight
            ConnectionGene connection = new ConnectionGene(id, source.getInnovationId(), target.getInnovationId(), getRandomDouble(-2, 2));
            genome.addConnection(connection);

            return;
        }
    }

    /**
     * Get a list of possible connection targets from a source neuron. This will exclude any invalid targets (input nodes) and
     * any neurons that already connected to the source.
     */
    private List<NeuronGene> getPossibleTargetsForNewConnection(Genome genome, NeuronGene source) {
        if (!genome.getNeurons().contains(source)) {
            throw new IllegalArgumentException("Source neuron is not a part of this genome");
        }

        List<NeuronGene> targets = getAvailableConnectionTargets(genome);

        //Remove source as a potential targets to prevent a self referencing neuron connection
        targets.remove(source);

        if (targets.size() == 0) {
            return targets;
        }

        //Remove existing connections and recurrent connections
        for (Iterator<NeuronGene> iterator = targets.iterator(); iterator.hasNext(); ) {
            NeuronGene target = iterator.next();
            if (genome.hasConnection(source, target)) {
                iterator.remove();
                continue;
            }

            if (source.getLayer() >= target.getLayer() && target.getType() != NeuronGeneType.OUTPUT && source.getType() != NeuronGeneType.INPUT) {
                iterator.remove();
            }
        }

        return targets;
    }

    /**
     * Get a list of all possible targets for a connection, this will include every type of neuron in the genome
     * except the input neurons
     */
    private List<NeuronGene> getAvailableConnectionTargets(Genome genome) {
        return genome.getNeurons()
                .stream()
                .filter(neuron -> neuron.getType() != NeuronGeneType.INPUT)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all possible sources for a connection, this will include every type of neuron in the genome
     * except the output neurons
     */
    private List<NeuronGene> getAvailableConnectionSources(Genome genome) {
        return genome.getNeurons()
                .stream()
                .filter(neuron -> neuron.getType() != NeuronGeneType.OUTPUT)
                .collect(Collectors.toList());
    }
}
