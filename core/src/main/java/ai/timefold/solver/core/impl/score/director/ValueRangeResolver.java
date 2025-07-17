package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.exception.EmptyRangeException;

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
    private Map<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<?>>> fromEntityMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> ValueRange<T> extractValueRangeFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @NonNull Solution_ solution) {
        var valueRange = fromSolutionMap.computeIfAbsent(valueRangeDescriptor,
                descriptor -> descriptor.extractValueRange(Objects.requireNonNull(solution), null));
        return (ValueRange<T>) valueRange;
    }

    @SuppressWarnings("unchecked")
    public <T> ValueRange<T> extractValueRangeFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @NonNull Object entity) {
        var valueRangeMap = fromEntityMap.computeIfAbsent(entity, e -> {
            var map = new IdentityHashMap<ValueRangeDescriptor<Solution_>, ValueRange<?>>();
            var range = valueRangeDescriptor.extractValueRange(null, Objects.requireNonNull(entity));
            map.put(valueRangeDescriptor, range);
            return map;
        });
        return (ValueRange<T>) valueRangeMap.computeIfAbsent(valueRangeDescriptor,
                descriptor -> valueRangeDescriptor.extractValueRange(null, Objects.requireNonNull(entity)));
    }

    public long extractValueRangeSizeFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @NonNull Solution_ solution) {
        try {
            // We rely on the value range to fetch the size
            var valueRange = extractValueRangeFromSolution(valueRangeDescriptor, solution);
            if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                return countableValueRange.getSize();
            } else {
                // It is not countable, and we need to call the descriptor specifically
                return valueRangeDescriptor.extractValueRangeSize(Objects.requireNonNull(solution), null);
            }
        } catch (EmptyRangeException e) {
            // Attempting to retrieve a range from an empty value list will result in an EmptyRangeException,
            // while fetching only the size will return zero.
            return 0;
        }
    }

    public long extractValueRangeSizeFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, @NonNull Object entity) {
        try {
            // We rely on the value range to fetch the size
            var valueRange = extractValueRangeFromEntity(valueRangeDescriptor, entity);
            if (valueRange instanceof CountableValueRange<?> countableValueRange) {
                return countableValueRange.getSize();
            } else {
                // It is not countable, and we need to call the descriptor specifically
                return valueRangeDescriptor.extractValueRangeSize(null, Objects.requireNonNull(entity));
            }
        } catch (EmptyRangeException e) {
            // Attempting to retrieve a range from an empty value list will result in an EmptyRangeException,
            // while fetching only the size will return zero.
            return 0;
        }
    }

    public void reset(@Nullable Solution_ workingSolution) {
        if (workingSolution == null || workingSolution != cachedWorkingSolution) {
            fromSolutionMap.clear();
            fromEntityMap.clear();
            // We only update the cached solution if it is not null
            if (workingSolution != null) {
                cachedWorkingSolution = workingSolution;
            }
        }
    }

    public void resize(int entityCount) {
        var resizedFromEntityMap =
                new IdentityHashMap<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<?>>>(entityCount);
        resizedFromEntityMap.putAll(fromEntityMap);
        this.fromEntityMap = resizedFromEntityMap;
    }
}
