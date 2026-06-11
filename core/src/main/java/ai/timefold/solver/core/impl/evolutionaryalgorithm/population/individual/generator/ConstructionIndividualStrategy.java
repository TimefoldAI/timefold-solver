package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.Phase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base contract for defining individuals' creation operations.
 */
@NullMarked
public interface ConstructionIndividualStrategy<Solution_, Score_ extends Score<Score_>> {

    Individual<Solution_, Score_> apply(EvolutionaryAlgorithmStepScope<Solution_> stepScope);

    Phase<Solution_> getLocalSearchPhase();

    @Nullable
    Phase<Solution_> getRefinementPhase();
}
