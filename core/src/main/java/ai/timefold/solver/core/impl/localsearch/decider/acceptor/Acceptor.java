package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.Nullable;

/**
 * An Acceptor accepts or rejects a selected {@link Move}.
 * Note that the {@link LocalSearchForager} can still ignore the advice of the {@link Acceptor}.
 *
 * @see AbstractAcceptor
 */
public interface Acceptor<Solution_> extends LocalSearchPhaseLifecycleListener<Solution_> {

    /**
     * Returns the lowest score that this acceptor would accept, or null if it could accept any score.
     * 
     * @return null if can accept a step with any store, the minimum score of a potential step otherwise.
     * @param <Solution_> The solution type
     * @param <Score_> The score type
     */
    @Nullable
    default <Score_ extends Score<Score_>> Score_ acceptedScoreLowerBound(LocalSearchStepScope<Solution_> stepScope) {
        return null;
    }

    /**
     * @param moveScope not null
     * @return true if accepted
     */
    boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope);

}
