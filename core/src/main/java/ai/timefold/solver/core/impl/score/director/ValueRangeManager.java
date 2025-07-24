package ai.timefold.solver.core.impl.score.director;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.EmptyValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.primdouble.DoubleValueRange;
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
 * <p>
 * Two score directors can never share the same instance of this class;
 * this class contains state that is specific to a particular instance of a working solution.
 * Even a clone of that same solution must not share the same instance of this class,
 * unless {@link #reset(Object)} is called with the clone;
 * failing to follow this rule will result in score corruptions as the cached value ranges reference
 * objects from the original working solution pre-clone.
 *
 * @see CountableValueRange
 * @see ValueRangeProvider
 */
@NullMarked
public final class ValueRangeManager<Solution_> {

    private @Nullable Solution_ cachedWorkingSolution = null;
    private final Map<ValueRangeDescriptor<Solution_>, CountableValueRange<?>> fromSolutionMap = new IdentityHashMap<>();
    private final Map<Object, Map<ValueRangeDescriptor<Solution_>, CountableValueRange<?>>> fromEntityMap =
            new IdentityHashMap<>();

    /**
     * As {@link #getFromSolution(ValueRangeDescriptor, Object)}, but the solution is taken from the cached working solution.
     * This requires {@link #reset(Object)} to be called before the first call to this method,
     * and therefore this method will throw an exception if called before the score director is instantiated.
     *
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    public <T> CountableValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: value range (%s) requested before the working solution is known."
                            .formatted(valueRangeDescriptor));
        }
        return getFromSolution(valueRangeDescriptor, cachedWorkingSolution);
    }

    @SuppressWarnings("unchecked")
    public <T> CountableValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Solution_ solution) {
        var valueRange = fromSolutionMap.get(valueRangeDescriptor);
        if (valueRange == null) { // Avoid computeIfAbsent on the hot path; creates capturing lambda instances.
            var extractedValueRange = valueRangeDescriptor.<T> extractAllValues(Objects.requireNonNull(solution));
            if (!(extractedValueRange instanceof CountableValueRange<T> countableValueRange)) {
                throw new UnsupportedOperationException("""
                        Impossible state: value range (%s) on planning solution (%s) is not countable.
                        Maybe replace %s with %s."""
                        .formatted(valueRangeDescriptor, solution, DoubleValueRange.class.getSimpleName(),
                                BigDecimalValueRange.class.getSimpleName()));
            } else if (valueRangeDescriptor.acceptsNullInValueRange()) {
                valueRange = new NullAllowingCountableValueRange<>(countableValueRange);
            } else if (extractedValueRange instanceof EmptyValueRange<?>) {
                throw new IllegalStateException("""
                        The @%s-annotated member (%s) on planning solution (%s) must not return an empty range.
                        Maybe apply over-constrained planning as described in the documentation."""
                        .formatted(ValueRangeProvider.class.getSimpleName(), valueRangeDescriptor, solution));
            } else {
                valueRange = countableValueRange;
            }
            fromSolutionMap.put(valueRangeDescriptor, valueRange);
        }
        return (CountableValueRange<T>) valueRange;
    }

    /**
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    @SuppressWarnings("unchecked")
    public <T> CountableValueRange<T> getFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: value range (%s) on planning entity (%s) requested before the working solution is known."
                            .formatted(valueRangeDescriptor, entity));
        }
        var valueRangeMap = fromEntityMap.computeIfAbsent(entity, e -> new IdentityHashMap<>());
        var valueRange = valueRangeMap.get(valueRangeDescriptor);
        if (valueRange == null) { // Avoid computeIfAbsent on the hot path; creates capturing lambda instances.
            var extractedValueRange =
                    valueRangeDescriptor.<T> extractValuesFromEntity(cachedWorkingSolution, Objects.requireNonNull(entity));
            if (!(extractedValueRange instanceof CountableValueRange<T> countableValueRange)) {
                throw new UnsupportedOperationException("""
                        Impossible state: value range (%s) on planning entity (%s) is not countable.
                        Maybe replace %s with %s."""
                        .formatted(valueRangeDescriptor, entity, DoubleValueRange.class.getSimpleName(),
                                BigDecimalValueRange.class.getSimpleName()));
            } else if (valueRangeDescriptor.acceptsNullInValueRange()) {
                valueRange = new NullAllowingCountableValueRange<>(countableValueRange);
            } else if (extractedValueRange instanceof EmptyValueRange<?>) {
                throw new IllegalStateException("""
                        The @%s-annotated member (%s) on planning entity (%s) must not return an empty range.
                        Maybe apply over-constrained planning as described in the documentation."""
                        .formatted(ValueRangeProvider.class.getSimpleName(), valueRangeDescriptor, entity));
            } else {
                valueRange = countableValueRange;
            }
            valueRangeMap.put(valueRangeDescriptor, valueRange);
        }
        return (CountableValueRange<T>) valueRange;
    }

    public long countOnSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ solution) {
        return getFromSolution(valueRangeDescriptor, solution)
                .getSize();
    }

    public long countOnEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        return getFromEntity(valueRangeDescriptor, entity)
                .getSize();
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

}
