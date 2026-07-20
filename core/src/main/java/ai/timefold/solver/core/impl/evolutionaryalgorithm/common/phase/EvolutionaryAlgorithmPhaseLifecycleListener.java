package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.solver.event.SolverLifecycleListener;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface EvolutionaryAlgorithmPhaseLifecycleListener<Solution_> extends SolverLifecycleListener<Solution_> {

    void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    void stepStarted(EvolutionaryAlgorithmStepScope<Solution_> stepScope);

    void stepEnded(EvolutionaryAlgorithmStepScope<Solution_> stepScope);

}
