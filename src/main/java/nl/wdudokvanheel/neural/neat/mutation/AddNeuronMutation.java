package nl.wdudokvanheel.neural.neat.mutation;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import nl.wdudokvanheel.neural.neat.service.InnovationService;

import java.util.ArrayList;
import java.util.List;

public class AddNeuronMutation extends AbstractMutation {
    private InnovationService innovationService;

    public AddNeuronMutation(InnovationService innovationService) {
        this.innovationService = innovationService;
    }

    @Override
    public void mutate(Genome genome) {
        if (genome.getConnections().isEmpty()) {
            return;
        }

        List<ConnectionGene> connections = new ArrayList<>(genome.getConnections());

        //Keep trying to replace a connection with a neuron until it succeeds or when there are no more possible connections to replace
        while (!connections.isEmpty()) {
            ConnectionGene connection = getRandomElement(connections);

            //Check if the connection has been created before, to avoid making an unused id
            if (innovationService.doesNeuronIdExist(connection.getInnovationId())) {
                //Innovation already exists, get its id now and make sure it doesn't exist yet in the genome
                int id = innovationService.getNeuronInnovationId(connection.getInnovationId());

                //This connection was already replaced by a neuron, so discard this connection as a possible connection to replace
                if (genome.getNeuronById(id) != null) {
                    connections.remove(connection);
                    continue;
                }
            }

            replaceConnectionWithNeuron(genome, connection);
            return;
        }
    }

    public void replaceConnectionWithNeuron(Genome genome, ConnectionGene connection) {
        // Ensure the connection is part of this genome
        if (!genome.getConnections().contains(connection)) {
            throw new IllegalArgumentException("Connection is not a part of the genome");
        }

        int newNeuronId = innovationService.getNeuronInnovationId(connection.getInnovationId());

        /*
           If this connection has already been split once, the InnovationService
           will return the same neuron-id again.  If that neuron is already
           present in the genome we simply return, preventing duplicate nodes.
       */
        if (genome.hasNeuron(newNeuronId)) {
            return;
        }

        //Disable the connection that is being replaced by a neuron
        connection.setEnabled(false);

        //Find the source neuron to get the layer
        NeuronGene source = genome.getNeuronById(connection.getSource());
        NeuronGene target = genome.getNeuronById(connection.getTarget());

        if (target.getLayer() - source.getLayer() <= 1) {
            moveNeuronsOneLayer(genome, target.getLayer());
        }

        //Create the new neuron
        NeuronGene newNeuron = new NeuronGene(
                NeuronGeneType.HIDDEN,
                newNeuronId,
                source.getLayer() + 1
        );
        genome.addNeuron(newNeuron);

        //Create a connection from the original source to the new neuron with a random weigth
        double init = 1.0;
        int sourceConnectionId = innovationService.getConnectionInnovationId(connection.getSource(), newNeuron.getInnovationId());
        ConnectionGene sourceConnection = new ConnectionGene(sourceConnectionId, connection.getSource(), newNeuron.getInnovationId(), init);
        genome.addConnection(sourceConnection);

        //Create a connection from the new neuron to the original destination with the same weight as the original connection
        int targetConnectionId = innovationService.getConnectionInnovationId(newNeuron.getInnovationId(), connection.getTarget());
        ConnectionGene targetConnection = new ConnectionGene(targetConnectionId, newNeuron.getInnovationId(), connection.getTarget(), connection.getWeight());
        genome.addConnection(targetConnection);
    }

    private void moveNeuronsOneLayer(Genome genome, int fromLayer) {
        for (NeuronGene neuron : genome.getNeurons()) {
            if (neuron.getLayer() >= fromLayer) {
                neuron.setLayer(neuron.getLayer() + 1);
            }
        }
    }
}
