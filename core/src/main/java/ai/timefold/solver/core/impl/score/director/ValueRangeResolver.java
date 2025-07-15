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
 * Call {@link #reset(Object)} every time the working solution changes through a problem fact,
 * so that all caches can be invalidated.
 *
 * @see ValueRange
 */
@NullMarked
public final class ValueRangeResolver<Solution_> {

    private @Nullable Solution_ cachedWorkingSolution = null;
    private final Map<ValueRangeDescriptor<Solution_>, ValueRange<?>> fromSolutionMap = new IdentityHashMap<>();

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
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            // No need to use the cache since there will be no additional overhead to compute the range
            return valueRangeDescriptor.extractValueRange(Objects.requireNonNull(solution), valueRangeDescriptor);
        } else {
            var valueRange = fromSolutionMap.computeIfAbsent(valueRangeDescriptor,
                    descriptor -> descriptor.extractValueRange(Objects.requireNonNull(solution), null));
            return (ValueRange<T>) valueRange;
        }
    }

    private <T> ValueRange<T> fetchValueRangeForEntity(@NonNull Object entity,
            ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        // Experiments indicate
        // that caching value ranges for large datasets is less effective
        // than recomputing the value range.
        // Therefore, no cache is used for entity value ranges.
        return valueRangeDescriptor.extractValueRange(null, entity);
    }

    public long extractValueRangeSize(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @Nullable Solution_ solution,
            @Nullable Object entity) {
        if (solution != null) {
            if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
                // No need to use the cache since there will be no additional overhead to compute the range
                return valueRangeDescriptor.extractValueRangeSize(solution, null);
            } else {
                var valueRange = (CountableValueRange<Solution_>) fetchValueRangeForSolution(solution, valueRangeDescriptor);
                return valueRange.getSize();
            }
        } else if (entity != null) {
            // There is no need to cache as getting the size requires no complex calculation
            return valueRangeDescriptor.extractValueRangeSize(null, entity);
        }
        throw new IllegalStateException(
                "The value range cannot be retrieved because both the solution and entity instances are null");
    }

    public void reset(@Nullable Solution_ workingSolution) {
        if (workingSolution == null || workingSolution != cachedWorkingSolution) {
            fromSolutionMap.clear();
            // We only update the cached solution if it is not null
            if (workingSolution != null) {
                cachedWorkingSolution = workingSolution;
            }
        }
    }
}
