package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.preview.api.move.Move;

public class MoveTabuAcceptor<Solution_> extends AbstractTabuAcceptor<Solution_> {

    public MoveTabuAcceptor(String logIndentation) {
        super(logIndentation);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    protected Collection<? extends Object> findTabu(LocalSearchMoveScope<Solution_> moveScope) {
        return Collections.singletonList(moveScope.getMove());
    }

    @Override
    protected Collection<? extends Object> findNewTabu(LocalSearchStepScope<Solution_> stepScope) {
        return Collections.singletonList((Move<?>) stepScope.getStep());
    }

}
