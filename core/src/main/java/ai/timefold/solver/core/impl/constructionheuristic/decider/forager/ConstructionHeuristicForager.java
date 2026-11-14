package ai.timefold.solver.core.impl.constructionheuristic.decider.forager;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.constructionheuristic.event.ConstructionHeuristicPhaseLifecycleListener;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicMoveScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @see AbstractConstructionHeuristicForager
 */
@NullMarked
public interface ConstructionHeuristicForager<Solution_>
        extends ConstructionHeuristicPhaseLifecycleListener<Solution_> {

    void addMove(ConstructionHeuristicMoveScope<Solution_> moveScope);

    boolean isQuitEarly();

    /**
     * The minimum score of the solution after a move for that move to be considered by
     * {@link #pickMove(ConstructionHeuristicStepScope)}.
     * This is typically the highest scoring move's score found so far within the current step, as that move
     * is typically picked if no better scoring moves are found.
     *
     * @return The minimum score that a move must reach in order to be picked by this {@link ConstructionHeuristicForager}.
     * @param <Score_> The solution's score type
     * @see Acceptor#getAcceptedMoveScoreLowerBound(LocalSearchStepScope, boolean)
     */
    @Nullable
    default <Score_ extends Score<Score_>> Score_ getForagedMoveScoreLowerBound() {
        return null;
    }

    ConstructionHeuristicMoveScope<Solution_> pickMove(ConstructionHeuristicStepScope<Solution_> stepScope);

}
