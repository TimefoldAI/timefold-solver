package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import java.util.List;

import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;

/**
 * A podium gathers the finalists (the {@link LocalSearchMoveScope}s which might win) and picks the winner.
 *
 * @see AbstractFinalistPodium
 * @see HighestScoreFinalistPodium
 */
public interface FinalistPodium<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * See {@link LocalSearchForager#addMove(LocalSearchMoveScope)}.
     *
     * @param moveScope never null
     */
    void addMove(LocalSearchMoveScope<Solution_> moveScope);

    /**
     *
     * @return never null, sometimes empty
     */
    List<LocalSearchMoveScope<Solution_>> getFinalistList();

}
