package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public interface TabuSizeStrategy<Solution_> {

    /**
     * @param stepScope never null
     * @return {@code >= 0}
     */
    int determineTabuSize(LocalSearchStepScope<Solution_> stepScope);

}
