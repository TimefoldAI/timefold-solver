package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public final class FixedTabuSizeStrategy<Solution_> extends AbstractTabuSizeStrategy<Solution_> {

    private final int tabuSize;

    public FixedTabuSizeStrategy(int tabuSize) {
        this.tabuSize = tabuSize;
        if (tabuSize < 1) {
            throw new IllegalArgumentException("The tabuSize (%d) must be at least 1."
                    .formatted(tabuSize));
        }
    }

    @Override
    public int determineTabuSize(LocalSearchStepScope<Solution_> stepScope) {
        return tabuSize;
    }

}
