package ai.timefold.solver.core.impl.constructionheuristic.decider.forager;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.constructionheuristic.event.ConstructionHeuristicPhaseLifecycleListener;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicMoveScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;

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

    @Nullable
    default <Score_ extends Score<Score_>> Score_ scoreLowerBound() {
        return null;
    }

    ConstructionHeuristicMoveScope<Solution_> pickMove(ConstructionHeuristicStepScope<Solution_> stepScope);

}
