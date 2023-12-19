package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.solver.core.api.domain.entity.PlanningPinToIndex;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;

public record PlanningPinToIndexReader<Solution_>(SelectionFilter<Solution_, Object> movableEntityFilter,
        Function<Object, List<?>> planningListVariableAccessor, MemberAccessor... planningPinIndexAccessors)
        implements
            ToIntBiFunction<ScoreDirector<Solution_>, Object> {

    @Override
    public int applyAsInt(ScoreDirector<Solution_> scoreDirector, Object o) {
        var planningListVariable = planningListVariableAccessor.apply(o);
        var planningListVariableSize = planningListVariable.size();
        var effectivePlanningPinToIndex = 0;
        for (var planningPinIndexAccessor : planningPinIndexAccessors) {
            var planningPinToIndex = (int) planningPinIndexAccessor.executeGetter(o);
            if (planningPinToIndex < 0) {
                throw new IllegalStateException(
                        "The entity (%s) has a @%s annotated property (%s) that returns a value (%s) that is less than zero."
                                .formatted(o, PlanningPinToIndex.class.getSimpleName(), planningPinIndexAccessor,
                                        planningPinToIndex));
            } else if (planningPinToIndex > planningListVariableSize) {
                throw new IllegalStateException(
                        "The entity (%s) has a @%s annotated property (%s) that returns a value (%s) that is higher than the size of the list variable (%s)."
                                .formatted(o, PlanningPinToIndex.class.getSimpleName(), planningPinIndexAccessor,
                                        planningPinToIndex, planningListVariableSize));
            } else {
                effectivePlanningPinToIndex = Math.max(effectivePlanningPinToIndex, planningPinToIndex);
            }
        }
        return effectivePlanningPinToIndex;
    }
}
