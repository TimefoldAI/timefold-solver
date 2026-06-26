package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

record NoOpLevelScoreState<Solution_>() implements LevelScoreState<Solution_> {

    @Override
    public void update(LocalSearchStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @Override
    public boolean isNonDominatedLevelChanged(LocalSearchStepScope<Solution_> stepScope) {
        return false;
    }
}
