package ai.timefold.solver.core.impl.localsearch.decider.acceptor.reconfiguration;

import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

public interface ReconfigurationStrategy<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    boolean needReconfiguration(LocalSearchMoveScope<Solution_> moveScope);

    void reset();
}
