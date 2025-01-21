package ai.timefold.solver.core.impl.localsearch.decider.perturbation;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class NoPerturbationStrategy<Solution_> implements PerturbationStrategy<Solution_> {
    @Override
    public <Score_ extends Score<Score_>> Score_ apply(AbstractStepScope<Solution_> stepScope) {
        throw new IllegalStateException("Impossible state");
    }

    @Override
    public boolean isTriggered(AbstractStepScope<Solution_> stepScope) {
        return false;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }
}
