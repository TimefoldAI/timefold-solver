package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.empty.EmptyValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Caches value ranges for the current working solution,
 * allowing to quickly access these cached value ranges when needed.
 * 
 * <p>
 * Outside a {@link ProblemChange}, value ranges are not allowed to change.
 * Call {@link #reset(Object)} every time the working solution changes through a problem fact,
 * so that all caches can be invalidated.
 *
 * @see ValueRange
 */
@NullMarked
public final class ValueRangeManager<Solution_> {

    private @Nullable Solution_ cachedWorkingSolution = null;
    private final Map<ValueRangeDescriptor<Solution_>, ValueRange<?>> fromSolutionMap = new IdentityHashMap<>();
    private final Map<Object, Map<ValueRangeDescriptor<Solution_>, ValueRange<?>>> fromEntityMap = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> ValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ solution) {
        var valueRange = fromSolutionMap.computeIfAbsent(valueRangeDescriptor,
                descriptor -> {
                    var extractedValueRange = descriptor.<T> extractValueRange(Objects.requireNonNull(solution), null);
                    if (valueRangeDescriptor.acceptNullInValueRange()) {
                        return checkForNullValues(descriptor, extractedValueRange);
                    } else {
                        if (extractedValueRange instanceof EmptyValueRange<?>) {
                            throw getOnSolutionRangeEmptyException(valueRangeDescriptor, solution);
                        }
                        return extractedValueRange;
                    }
                });
        return (ValueRange<T>) valueRange;
    }

    private static IllegalStateException getOnSolutionRangeEmptyException(ValueRangeDescriptor<?> valueRangeDescriptor,
            Object solution) {
        return new IllegalStateException("""
                The @%s-annotated member (%s) on planning solution (%s) must not return an empty range.
                Maybe apply over-constrained planning as described in the documentation."""
                .formatted(ValueRangeProvider.class.getSimpleName(), valueRangeDescriptor, solution));
    }

    @SuppressWarnings("unchecked")
    public <T> ValueRange<T> getFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        var valueRangeMap = fromEntityMap.computeIfAbsent(entity, e -> {
            var entityMap = new IdentityHashMap<ValueRangeDescriptor<Solution_>, ValueRange<?>>();
            var extractedValueRange = valueRangeDescriptor.<T> extractValueRange(null, Objects.requireNonNull(entity));
            entityMap.put(valueRangeDescriptor, checkForNullValues(valueRangeDescriptor, extractedValueRange));
            return entityMap;
        });
        return (ValueRange<T>) valueRangeMap.computeIfAbsent(valueRangeDescriptor,
                descriptor -> {
                    var extractedValueRange = valueRangeDescriptor.<T> extractValueRange(null, Objects.requireNonNull(entity));
                    if (valueRangeDescriptor.acceptNullInValueRange()) {
                        return checkForNullValues(descriptor, extractedValueRange);
                    } else {
                        if (extractedValueRange instanceof EmptyValueRange<?>) {
                            throw getOnEntityRangeEmptyException(valueRangeDescriptor, entity);
                        }
                        return extractedValueRange;
                    }
                });
    }

    private static IllegalStateException getOnEntityRangeEmptyException(ValueRangeDescriptor<?> valueRangeDescriptor,
            Object entity) {
        return new IllegalStateException("""
                The @%s-annotated member (%s) on planning entity (%s) must not return an empty range.
                Maybe apply over-constrained planning as described in the documentation."""
                .formatted(ValueRangeProvider.class.getSimpleName(), valueRangeDescriptor, entity));
    }

    public long countOnSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ solution) {
        // We rely on the value range to fetch the size
        var valueRange = getFromSolution(valueRangeDescriptor, solution);
        if (valueRange instanceof EmptyValueRange) {
            if (valueRangeDescriptor.acceptNullInValueRange()) {
                return 1; // Empty value range with null allowed counts as size 1
            } else {
                throw getOnSolutionRangeEmptyException(valueRangeDescriptor, solution);
            }
        } else if (valueRange instanceof CountableValueRange<?> countableValueRange) {
            return countableValueRange.getSize();
        } else {
            // It is not countable, and we need to call the descriptor specifically
            var size = valueRangeDescriptor.extractValueRangeSize(Objects.requireNonNull(solution), null);
            if (valueRangeDescriptor.acceptNullInValueRange()) {
                size++;
            }
            return size;
        }
    }

    public long countOnEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        // We rely on the value range to fetch the size
        var valueRange = getFromEntity(valueRangeDescriptor, entity);
        if (valueRange instanceof EmptyValueRange) {
            if (valueRangeDescriptor.acceptNullInValueRange()) {
                return 1; // Empty value range with null allowed counts as size 1
            } else {
                throw getOnEntityRangeEmptyException(valueRangeDescriptor, entity);
            }
        } else if (valueRange instanceof CountableValueRange<?> countableValueRange) {
            return countableValueRange.getSize();
        } else {
            // It is not countable, and we need to call the descriptor specifically
            var size = valueRangeDescriptor.extractValueRangeSize(null, Objects.requireNonNull(entity));
            if (valueRangeDescriptor.acceptNullInValueRange()) {
                size++;
            }
            return size;
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

    private <T> ValueRange<T> checkForNullValues(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            ValueRange<T> valueRange) {
        if (valueRangeDescriptor.acceptNullInValueRange()
                && valueRange instanceof CountableValueRange<T> countableValueRange) {
            return new NullAllowingCountableValueRange<>(countableValueRange);
        }
        return valueRange;
    }
}
