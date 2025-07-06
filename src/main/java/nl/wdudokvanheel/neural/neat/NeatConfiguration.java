package nl.wdudokvanheel.neural.neat;

public class NeatConfiguration {
    public int populationSize = 1000;
    public int minimumSpeciesSizeForChampionCopy = 5;
    public boolean copyChampionsAllSpecies = true;
    public double speciesThreshold = 3.0;
    public int targetSpecies = 50;
    public boolean adjustSpeciesThreshold = true;
    public double minSpeciesThreshold = 0.3;
    public double maxSpeciesThreshold = 10.0;
    public double bottomElimination = 0.2;
    public double reproduceWithoutCrossover = 0.25;
    public double newCreaturesPerGeneration = 0.2;
    public double interspeciesCrossover = 0.001;
    public boolean setInitialLinks = false;
    public double initialLinkActiveProbability = 0.25;

    //Mutation chances
    public boolean multipleMutationsPerGenome = true;
    public double mutateAddConnectionProbability = 0.05;
    public double mutateAddNeuronProbability = 0.03;
    public double mutateToggleConnectionProbability = 0.01;
    public double mutateWeightProbability = 0.8;
    public double mutateConnectionWeightProbability = 0.5;
    public double mutateRandomizeWeightsProbability = 0.1;
    public double mutateWeightPerturbationPower = 0.5;
    public boolean eliminateStagnantSpecies = true;
}
