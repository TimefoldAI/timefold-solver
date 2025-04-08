package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class MoveTabuAcceptor<Solution_> extends AbstractTabuAcceptor<Solution_> {

    public MoveTabuAcceptor(String logIndentation) {
        super(logIndentation);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    protected Collection<Object> findTabu(LocalSearchMoveScope<Solution_> moveScope) {
        return Collections.singletonList(moveScope.getMove());
    }

    @Override
    protected Collection<Object> findNewTabu(LocalSearchStepScope<Solution_> stepScope) {
        return Collections.singletonList(stepScope.getStep());
    }

}
