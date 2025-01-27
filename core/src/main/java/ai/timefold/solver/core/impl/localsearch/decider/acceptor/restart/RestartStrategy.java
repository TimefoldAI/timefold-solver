package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

public interface RestartStrategy<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    boolean isTriggered(LocalSearchMoveScope<Solution_> moveScope);
}
