package nl.wdudokvanheel.neural.neat;

import nl.wdudokvanheel.neural.neat.model.ConnectionGene;
import nl.wdudokvanheel.neural.neat.model.Genome;
import nl.wdudokvanheel.neural.neat.model.NeuronGene;
import nl.wdudokvanheel.neural.neat.model.NeuronGeneType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenomeComparison {
    private Logger logger = LoggerFactory.getLogger(GenomeComparison.class);

    private double distance = 0;

    private Genome fitParent;
    private Genome weakParent;

    //C1 in the NEAT paper
    private double excessCoefficient = 1.0;
    //C2 in the NEAT paper
    private double disjointCoefficient = 1.0;
    //C3 in the NEAT paper
    private double weightCoefficient = 2.0;

    private int totalGenes = 0;

    private int matchingGenes = 0;
    private int matchingNeurons = 0;
    private int matchingConnections = 0;

    private int excessGenes = 0;
    private int excessNeurons = 0;
    private int excessConnections = 0;

    private int disjointGenes = 0;
    private int disjointConnections = 0;
    private int disjointNeurons = 0;

    private double averageWeightDifference = 0;

    public GenomeComparison(Genome parentA, Genome parentB) {
        this.fitParent = parentA;
        this.weakParent = parentB;
        calculateValues();
    }

    public GenomeComparison(Genome parentA, Genome parentB, double excessCoefficient, double disjointCoefficient, double weightCoefficient) {
        this.fitParent = parentA;
        this.weakParent = parentB;
        this.excessCoefficient = excessCoefficient;
        this.disjointCoefficient = disjointCoefficient;
        this.weightCoefficient = weightCoefficient;
    }

    private void calculateValues() {
        calculateNeuronValues();
        calculateConnectionValues();

        totalGenes = Math.max(countTotalGenes(fitParent), countTotalGenes(weakParent));
        matchingGenes = matchingNeurons + matchingConnections;

        excessGenes = excessNeurons + excessConnections;
        disjointGenes = disjointNeurons + disjointConnections;

        int n = Math.max(totalGenes, 1);

        distance = (excessCoefficient * excessGenes / n) + (disjointCoefficient * disjointGenes / n) + (weightCoefficient * averageWeightDifference);
    }

    private void calculateNeuronValues() {
        int maxInnovationIdFitParent = getMaxNeuronInnovationId(fitParent);
        int maxInnovationIdWeakParent = getMaxNeuronInnovationId(weakParent);
        int maxInnovationId = Math.max(maxInnovationIdFitParent, maxInnovationIdWeakParent);

        for (int i = 1; i <= maxInnovationId; i++) {
            NeuronGene fitNeuron = fitParent.getNeuronById(i);
            NeuronGene weakNeuron = weakParent.getNeuronById(i);

            //Continue if both genomes don't have the neuron
            if (fitNeuron == null && weakNeuron == null) {
                continue;
            }

            //Skip any neurons that are not hidden, as input and output nodes should be the same for both genomes
            if ((fitNeuron != null && fitNeuron.getType() != NeuronGeneType.HIDDEN) || (weakNeuron != null && weakNeuron.getType() != NeuronGeneType.HIDDEN)) {
                continue;
            }

            //Both genomes have this neuron, so count it as a matching neuron
            if (fitNeuron != null && weakNeuron != null) {
                matchingNeurons++;
                continue;
            }

            //Only the fit parent has the neuron, test if it's an excess or disjoint neuron
            if (fitNeuron != null) {
                if (fitNeuron.getInnovationId() > maxInnovationIdWeakParent) {
                    excessNeurons++;
                } else {
                    disjointNeurons++;
                }
                continue;
            }

            //Only the weak parent has the neuron, test if it's an excess or disjoint neuron
            if (weakNeuron != null) {
                if (weakNeuron.getInnovationId() > maxInnovationIdFitParent) {
                    excessNeurons++;
                } else {
                    disjointNeurons++;
                }
            }
        }
    }

    private void calculateConnectionValues() {
        int maxInnovationIdFitParent = getMaxConnectionInnovationId(fitParent);
        int maxInnovationIdWeakParent = getMaxConnectionInnovationId(weakParent);
        int maxInnovationId = Math.max(maxInnovationIdFitParent, maxInnovationIdWeakParent);

        double totalWeightDifference = 0;

        for (int i = 1; i <= maxInnovationId; i++) {
            ConnectionGene fitConnection = fitParent.getConnectionById(i);
            ConnectionGene weakConnection = weakParent.getConnectionById(i);

            if (fitConnection == null && weakConnection == null) {
                continue;
            }

            //Both genomes have this connection, so count it as a matching connection
            if (fitConnection != null && weakConnection != null) {
                matchingConnections++;
                //Add the weight difference to the total
                totalWeightDifference += Math.abs(fitConnection.getWeight() - weakConnection.getWeight());
                continue;
            }

            //Only the fit parent has the connection, test if it's an excess or disjoint connection
            if (fitConnection != null) {
                if (fitConnection.getInnovationId() > maxInnovationIdWeakParent) {
                    excessConnections++;
                } else {
                    disjointConnections++;
                }
                continue;
            }

            //Only the weak parent has the connection, test if it's an excess or disjoint connection
            if (weakConnection != null) {
                if (weakConnection.getInnovationId() > maxInnovationIdFitParent) {
                    excessConnections++;
                } else {
                    disjointConnections++;
                }
            }
        }

        averageWeightDifference = totalWeightDifference / matchingConnections;
    }

    private int getMaxNeuronInnovationId(Genome genome) {
        int highest = 0;
        for (NeuronGene neuron : genome.getNeurons()) {
            if (neuron.getInnovationId() > highest) {
                highest = neuron.getInnovationId();
            }
        }

        return highest;
    }

    private int getMaxConnectionInnovationId(Genome genome) {
        int highest = 0;
        for (ConnectionGene connection : genome.getConnections()) {
            if (connection.getInnovationId() > highest) {
                highest = connection.getInnovationId();
            }
        }
        return highest;
    }

    private int countTotalGenes(Genome genome) {
        int total = genome.getConnections().size();

        for (NeuronGene neuron : genome.getNeurons()) {
            if (neuron.getType() == NeuronGeneType.HIDDEN) {
                total++;
            }
        }

        return total;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "Genomes distance: " + distance + " disjoint: " + disjointGenes + " excess: " + excessGenes + " average weight diff: " + averageWeightDifference;
    }
}
