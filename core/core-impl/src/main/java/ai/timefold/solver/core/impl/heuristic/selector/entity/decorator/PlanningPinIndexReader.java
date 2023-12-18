package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.entity.PlanningPinIndex;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;

public record PlanningPinIndexReader<Solution_>(SelectionFilter<Solution_, Object> movableEntityFilter,
        Function<Object, List<?>> planningListVariableAccessor,
        MemberAccessor... planningPinIndexAccessors)
        implements
            BiFunction<ScoreDirector<Solution_>, Object, OptionalInt> {

    @Override
    public OptionalInt apply(ScoreDirector<Solution_> solution, Object o) {
        var planningListVariable = planningListVariableAccessor.apply(o);
        var planningListVariableSize = planningListVariable.size();
        var effectivePlanningPinIndex = -1;
        for (var planningPinIndexAccessor : planningPinIndexAccessors) {
            var planningPinIndex = (Integer) planningPinIndexAccessor.executeGetter(o);
            if (planningPinIndex == null || planningPinIndex < 0) {
                continue;
            } if (planningPinIndex >= planningListVariableSize) {
                throw new IllegalStateException(
                        "The entity (%s) has a @%s annotated property (%s) that returns a value (%s) that is higher than the size of the list variable (%s)."
                                .formatted(o, PlanningPinIndex.class.getSimpleName(), planningPinIndexAccessor,
                                        planningPinIndex, planningListVariableSize));
            } else {
                effectivePlanningPinIndex = Math.max(effectivePlanningPinIndex, planningPinIndex);
            }
        }
        if (effectivePlanningPinIndex < 0) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(effectivePlanningPinIndex);
        }
    }
}
