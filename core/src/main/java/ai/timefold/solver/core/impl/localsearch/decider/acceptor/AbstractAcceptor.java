package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link Acceptor}.
 *
 * @see Acceptor
 */
public abstract class AbstractAcceptor<Solution_> extends LocalSearchPhaseLifecycleListenerAdapter<Solution_>
        implements Acceptor<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    @Override
    public final boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        if (moveScope.getScore().isInvalid()) {
            return false;
        }
        return isStructurallyValidSolutionAccepted(moveScope);
    }

    protected abstract boolean isStructurallyValidSolutionAccepted(LocalSearchMoveScope<Solution_> moveScope);

}
