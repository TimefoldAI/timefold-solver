package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Caches value ranges for the current working solution,
 * allowing to quickly access.
 * 
 * <p>
 * Outside a {@link ProblemChange}, value ranges are not allowed to change.
 * Call {@link #reset()} every time the working solution changes through a problem fact,
 * so that all caches can be invalidated.
 *
 * @see ValueRange
 */
@NullMarked
public final class ValueRangeState<Solution_> implements ValueRangeResolver<Solution_> {

    private final Map<ValueRangeDescriptor<Solution_>, ValueRange<?>> fromSolutionValueRangeMap = new IdentityHashMap<>();
    private final Map<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<?>>> fromEntityValueRangeMap =
            new IdentityHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <Value_> ValueRange<Value_> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @Nullable Solution_ solution, @Nullable Object entity) {
        if (valueRangeDescriptor.isEntityIndependent()) {
            var valueRange = fromSolutionValueRangeMap.get(valueRangeDescriptor);
            if (valueRange == null) {
                valueRange = valueRangeDescriptor.extractValueRange(Objects.requireNonNull(solution), null);
                fromSolutionValueRangeMap.put(valueRangeDescriptor, valueRange);
            }
            return (ValueRange<Value_>) valueRange;
        } else {
            var valueRangeMap = fromEntityValueRangeMap.get(Objects.requireNonNull(entity));
            if (valueRangeMap == null) {
                valueRangeMap = new IdentityHashMap<>();
                fromEntityValueRangeMap.put(entity, valueRangeMap);
            }
            var valueRange = (ValueRange<Value_>) valueRangeMap.get(valueRangeDescriptor);
            if (valueRange == null) {
                valueRange = valueRangeDescriptor.extractValueRange(solution, entity);
                valueRangeMap.put(valueRangeDescriptor, valueRange);
            }
            return valueRange;
        }
    }

    @Override
    public long extractValueRangeSize(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @Nullable Solution_ solution,
            @Nullable Object entity) {
        if (valueRangeDescriptor.isEntityIndependent()) {
            var valueRange = fromSolutionValueRangeMap.get(valueRangeDescriptor);
            if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                return countableValueRange.getSize();
            }
            return valueRangeDescriptor.extractValueRangeSize(solution, entity);
        } else {
            var valueRangeMap = fromEntityValueRangeMap.get(Objects.requireNonNull(entity));
            if (valueRangeMap != null) {
                var valueRange = valueRangeMap.get(entity);
                if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                    return countableValueRange.getSize();
                }
            }
            return valueRangeDescriptor.extractValueRangeSize(solution, entity);
        }
    }

    public void reset() {
        fromSolutionValueRangeMap.clear();
        fromEntityValueRangeMap.clear();
    }
}
