package nl.wdudokvanheel.neural.neat.service;

import nl.wdudokvanheel.neural.neat.genome.ConnectionGene;
import nl.wdudokvanheel.neural.neat.genome.Genome;

public class GenomeComparison {
    private double distance = 0;

    private Genome fitParent;
    private Genome weakParent;

    //C1 in the NEAT paper
    private double excessCoefficient = 1.0;
    //C2 in the NEAT paper
    private double disjointCoefficient = 1.0;
    //C3 in the NEAT paper
    private double weightCoefficient = 0.5;

    private int matchingConnections = 0;

    private int excessGenes = 0;
    private int excessConnections = 0;

    private int disjointGenes = 0;
    private int disjointConnections = 0;

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
        calculateConnectionValues();                       // neurons omitted

        int n = Math.max(
                fitParent.getConnections().size(),
                weakParent.getConnections().size()
        );
        if (n == 0) n = 1;                                 // avoid divide-by-zero

        distance = (excessCoefficient   * excessConnections   / n)
                + (disjointCoefficient * disjointConnections / n)
                + (weightCoefficient   * averageWeightDifference);
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

        averageWeightDifference = matchingConnections == 0 ? 0 : totalWeightDifference / matchingConnections;
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

    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "Genomes distance: " + distance + " disjoint: " + disjointGenes + " excess: " + excessGenes + " average weight diff: " + averageWeightDifference;
    }
}
