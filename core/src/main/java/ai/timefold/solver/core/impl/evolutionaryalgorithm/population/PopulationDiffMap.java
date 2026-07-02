package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;

/**
 * Stores information about individual differences, which are used for survival selection methods.
 */
public final class PopulationDiffMap<Solution_, Score_ extends Score<Score_>> {

    private final int size;
    private final Map<Individual<Solution_, Score_>, Map<Individual<Solution_, Score_>, Double>> individualMap;

    PopulationDiffMap(int maxPopulationSize) {
        this.size = maxPopulationSize;
        this.individualMap = new IdentityHashMap<>(maxPopulationSize);
    }

    /**
     * Add the computed diff between the source and target individuals to the diff map.
     * The method updates both sides of the relationship: source[target] and target[source].
     * 
     * @param source the source individual
     * @param target the target individual
     * @param diff the calculated diff
     */
    void addIndividualDiff(Individual<Solution_, Score_> source, Individual<Solution_, Score_> target, double diff) {
        var sourceDiffMap = individualMap.computeIfAbsent(source, k -> new IdentityHashMap<>(size));
        sourceDiffMap.put(target, diff);
        var targetDiffMap = individualMap.computeIfAbsent(target, k -> new IdentityHashMap<>(size));
        targetDiffMap.put(source, diff);
    }

    /**
     * Remove all diff computations associated to the given individual.
     *
     * @param individual the individual to be removed
     */
    void removeIndividualDiff(Individual<Solution_, Score_> individual) {
        individualMap.remove(individual);
        for (var diffMap : individualMap.values()) {
            diffMap.remove(individual);
        }
    }

    Map<Individual<Solution_, Score_>, Double> getIndividualDiffMap(Individual<Solution_, Score_> individual) {
        var map = individualMap.get(individual);
        return map != null ? map : Collections.emptyMap();
    }

    void clear() {
        individualMap.clear();
    }
}
