package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;

public record PlanningPinToIndexReader<Solution_>(SelectionFilter<Solution_, Object> movableEntityFilter,
        Function<Object, List<?>> planningListVariableAccessor, MemberAccessor... planningPinIndexAccessors)
        implements
            ToIntBiFunction<ScoreDirector<Solution_>, Object> {

    @Override
    public int applyAsInt(ScoreDirector<Solution_> scoreDirector, Object o) {
        var effectivePlanningPinToIndex = 0;
        for (var planningPinIndexAccessor : planningPinIndexAccessors) {
            var planningPinToIndex = (int) planningPinIndexAccessor.executeGetter(o);
            effectivePlanningPinToIndex = Math.max(effectivePlanningPinToIndex, planningPinToIndex);
        }
        return effectivePlanningPinToIndex;
    }
}
