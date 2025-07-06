## NEAT Configuration Options

The `NeatConfiguration` class defines parameters that control various aspects of the NEAT algorithm implementation.
Below is a comprehensive list of all configuration options, along with their purpose, type, and default values.

---

### Population & Species Management

* **`populationSize`** (`int`, default: `1000`)

    * Total number of creatures (individuals) maintained in the population each generation.

* **`minimumSpeciesSizeForChampionCopy`** (`int`, default: `5`)

    * Minimum number of creatures required in a species to allow copying its champion (best individual) into the next
      generation without mutation.

* **`copyChampionsAllSpecies`** (`boolean`, default: `true`)

    * Whether to copy the champion of each eligible species into the next generation.

* **`speciesThreshold`** (`double`, default: `3.0`)

    * Compatibility distance threshold: maximum genome distance for two individuals to belong to the same species.

* **`adjustSpeciesThreshold`** (`boolean`, default: `true`)

    * Whether to dynamically adjust `speciesThreshold` to steer the species count toward `targetSpecies`.

* **`targetSpecies`** (`int`, default: `50`)

    * Desired number of species; used when automatically adjusting the compatibility threshold.

* **`minSpeciesThreshold`** (`double`, default: `0.3`)

    * Lower bound for `speciesThreshold` when auto-adjusting.

* **`maxSpeciesThreshold`** (`double`, default: `10.0`)

    * Upper bound for `speciesThreshold` when auto-adjusting.

* **`bottomElimination`** (`double`, default: `0.2`)

    * Fraction of the least-fit individuals in each species to remove (cull) each generation.

---

### Reproduction Parameters

* **`reproduceWithoutCrossover`** (`double`, default: `0.25`)

    * Fraction of offspring produced asexually (cloning with mutation) vs. sexually (crossover).

* **`interspeciesCrossover`** (`double`, default: `0.001`)

    * Probability of performing crossover between two individuals from different species.

* **`newCreaturesPerGeneration`** (`double`, default: `0.2`)

    * Fraction of the population to replace with entirely new random creatures each generation (random initialization).

---

### Initial Population Setup

* **`setInitialLinks`** (`boolean`, default: `false`)

    * Whether to randomly enable or disable links in the initial blueprint-derived population.

* **`initialLinkActiveProbability`** (`double`, default: `0.25`)

    * Probability that a given link is active if `setInitialLinks` is enabled.

---

### Mutation Parameters

* **`multipleMutationsPerGenome`** (`boolean`, default: `true`)

    * If `true`, multiple mutation types (add connection, add neuron, toggle connection, weight mutation) may all fire
      on the same genome in one generation. If `false`, only one mutation is applied per genome.

* **`mutateAddConnectionProbability`** (`double`, default: `0.05`)

    * Probability of attempting an Add-Connection mutation.

* **`mutateAddNeuronProbability`** (`double`, default: `0.03`)

    * Probability of attempting an Add-Neuron (split connection) mutation.

* **`mutateToggleConnectionProbability`** (`double`, default: `0.01`)

    * Probability of toggling (enable/disable) an existing connection.

* **`mutateWeightProbability`** (`double`, default: `0.8`)

    * Probability of performing a weight mutation (either randomize or perturb weights).

* **`mutateRandomizeWeightsProbability`** (`double`, default: `0.1`)

    * Within weight mutations, probability of fully randomizing weights vs. perturbing them slightly.

* **`mutateWeightPerturbationPower`** (`double`, default: `0.5`)

    * Standard deviation multiplier for Gaussian perturbations when shifting weights.

* **`eliminateStagnantSpecies`** (`boolean`, default: `true`)

    * Whether to remove species that have not improved in fitness for a specified number of generations (stagnation).
