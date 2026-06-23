package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public interface LevelScoreState<Solution_> {

    void update(LocalSearchStepScope<Solution_> stepScope);

    boolean isNonDominatedLevelChanged(LocalSearchStepScope<Solution_> stepScope);
}
