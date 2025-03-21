package ai.timefold.solver.core.impl.move.streams;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

public record FromSolutionValueCollectingFunction<Solution_, A>(ValueRangeDescriptor<Solution_> valueRangeDescriptor)
        implements
            Function<Solution_, CountableValueRange<A>> {

    @Override
    public CountableValueRange<A> apply(Solution_ solution) {
        return ensureCountable(valueRangeDescriptor.extractValueRange(solution, null));
    }

    private static <A> CountableValueRange<A> ensureCountable(ValueRange<A> valueRange) {
        if (valueRange instanceof CountableValueRange<A> countableValueRange) {
            return countableValueRange;
        } else { // Non-countable value ranges cannot be enumerated.
            throw new UnsupportedOperationException("The value range (%s) is not countable."
                    .formatted(valueRange));
        }
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
