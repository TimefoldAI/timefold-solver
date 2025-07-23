package ai.timefold.solver.core.impl.move.streams;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;

public record FromSolutionValueCollectingFunction<Solution_, A>(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
        ValueRangeManager<Solution_> valueRangeManager)
        implements
            Function<Solution_, CountableValueRange<A>> {

    @Override
    public CountableValueRange<A> apply(Solution_ solution) {
        return valueRangeManager.getFromSolution(valueRangeDescriptor, solution);
    }

    @SuppressWarnings("unchecked")
    public Class<A> declaredClass() {
        return (Class<A>) valueRangeDescriptor.getVariableDescriptor().getVariablePropertyType();
    }

    @Override
    public String toString() {
        return "FromSolution(%s)"
                .formatted(valueRangeDescriptor.getVariableDescriptor());
    }

}
