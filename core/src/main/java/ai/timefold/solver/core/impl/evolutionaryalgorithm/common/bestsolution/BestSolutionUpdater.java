package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution;

import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface BestSolutionUpdater<Solution_> {

    void updateBestSolution(EvolutionaryAlgorithmStepScope<Solution_> stepScope);
}
