package ai.timefold.solver.core.impl.domain.entity.descriptor;

import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

@FunctionalInterface
public interface PlanningPinToIndexReader<Solution_>
        extends ToIntBiFunction<ScoreDirector<Solution_>, Object> {

}
