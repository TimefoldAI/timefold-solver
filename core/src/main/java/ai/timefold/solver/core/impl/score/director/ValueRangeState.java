package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

import org.jspecify.annotations.NonNull;
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

    private final Map<ValueRangeDescriptor<Solution_>, ValueRange<?>> fromSolutionMap = new IdentityHashMap<>();
    private final Map<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<?>>> fromEntityMap = new IdentityHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> ValueRange<T> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @Nullable Solution_ solution, @Nullable Object entity) {
        if (solution != null) {
            return fetchValueRangeForSolution(solution, valueRangeDescriptor);
        } else if (entity != null) {
            return fetchValueRangeForEntity(entity, valueRangeDescriptor);
        }
        throw new IllegalStateException(
                "The value range cannot be retrieved because both the solution and entity instances are null");
    }

    private <T> ValueRange<T> fetchValueRangeForSolution(@NonNull Solution_ solution,
            ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        var valueRange = fromSolutionMap.get(valueRangeDescriptor);
        if (valueRange == null) {
            valueRange = valueRangeDescriptor.extractValueRange(Objects.requireNonNull(solution), null);
            fromSolutionMap.put(valueRangeDescriptor, valueRange);
        }
        return (ValueRange<T>) valueRange;
    }

    private <T> ValueRange<T> fetchValueRangeForEntity(@NonNull Object entity,
            ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        var valueRangeMap = fromEntityMap.get(Objects.requireNonNull(entity));
        if (valueRangeMap == null) {
            valueRangeMap = new IdentityHashMap<>();
            fromEntityMap.put(entity, valueRangeMap);
        }
        var valueRange = (ValueRange<T>) valueRangeMap.get(valueRangeDescriptor);
        if (valueRange == null) {
            valueRange = valueRangeDescriptor.extractValueRange(null, entity);
            valueRangeMap.put(valueRangeDescriptor, valueRange);
        }
        return valueRange;
    }

    @Override
    public long extractValueRangeSize(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @Nullable Solution_ solution,
            @Nullable Object entity) {
        if (solution != null) {
            var valueRange = fromSolutionMap.get(valueRangeDescriptor);
            if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                return countableValueRange.getSize();
            }
            return valueRangeDescriptor.extractValueRangeSize(solution, null);
        } else if (entity != null) {
            var valueRangeMap = fromEntityMap.get(Objects.requireNonNull(entity));
            if (valueRangeMap != null) {
                var valueRange = valueRangeMap.get(entity);
                if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                    return countableValueRange.getSize();
                }
            }
            return valueRangeDescriptor.extractValueRangeSize(null, entity);
        }
        throw new IllegalStateException(
                "The value range cannot be retrieved because both the solution and entity instances are null");
    }

    public void reset() {
        fromSolutionMap.clear();
        fromEntityMap.clear();
    }
}
