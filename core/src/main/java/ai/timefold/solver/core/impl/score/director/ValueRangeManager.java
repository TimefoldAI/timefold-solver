package ai.timefold.solver.core.impl.score.director;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

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
 * @see ValueRange
 * @see ValueRangeProvider
 */
@NullMarked
public final class ValueRangeManager<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    // Single value range descriptor requires no array
    private @Nullable ValueRangeState<Solution_, ?, ?> singleValueRangeState;
    // Using multiple value range descriptors
    private final @Nullable ValueRangeState<Solution_, ?, ?> @Nullable [] multipleValueRangeState;
    private @Nullable Solution_ cachedWorkingSolution = null;
    private @Nullable ValueRangeStatistics<Solution_> statistics;

    public static <Solution_> ValueRangeManager<Solution_> of(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution) {
        var valueRangeManager = new ValueRangeManager<>(solutionDescriptor);
        valueRangeManager.reset(solution);
        return valueRangeManager;
    }

    /**
     * It is not recommended for code other than {@link ScoreDirector} to create instances of this class.
     * See class-level documentation for more details.
     * For safety, prefer using {@link #of(SolutionDescriptor, Object)} to create an instance of this class
     * with a solution already set.
     */
    public ValueRangeManager(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        var countDescriptor = solutionDescriptor.getValueRangeDescriptorCount();
        if (countDescriptor > 1) {
            this.multipleValueRangeState = new ValueRangeState[countDescriptor];
        } else {
            this.multipleValueRangeState = null;
        }
    }

    private ValueRangeStatistics<Solution_> ensureStatisticsInitialized(Solution_ solution) {
        if (statistics == null) {
            statistics = new ValueRangeStatistics<>(this, solutionDescriptor, solution);
        } else if (statistics.getSolution() != solution) {
            // Create a new instance as the solution differs
            // The cached solution from the statistics should only change if the manager is reset
            return new ValueRangeStatistics<>(this, solutionDescriptor, solution);
        }
        return statistics;
    }

    ValueRangeStatistics<Solution_> getStatistics() {
        return ensureStatisticsInitialized(Objects.requireNonNull(cachedWorkingSolution));
    }

    public SolutionInitializationStatistics getInitializationStatistics() {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: initialization statistics requested before the working solution is known.");
        }
        return getInitializationStatistics(null);
    }

    public SolutionInitializationStatistics getInitializationStatistics(@Nullable Consumer<Object> finisher) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: initialization statistics requested before the working solution is known.");
        }
        return ensureStatisticsInitialized(cachedWorkingSolution).computeInitializationStatistics(finisher, true);
    }

    public SolutionInitializationStatistics computeInitializationStatistics(Solution_ solution,
            @Nullable Consumer<Object> finisher) {
        return ensureStatisticsInitialized(solution).computeInitializationStatistics(finisher, false);
    }

    public ProblemSizeStatistics getProblemSizeStatistics() {
        return ensureStatisticsInitialized(Objects.requireNonNull(cachedWorkingSolution)).getProblemSizeStatistics();
    }

    private <Entity_, Value_> ValueRangeState<Solution_, Entity_, Value_>
            fromDescriptor(AbstractValueRangeDescriptor<Solution_> descriptor) {
        if (multipleValueRangeState == null) {
            // Null array means there are only one variable range descriptor
            if (singleValueRangeState == null) {
                singleValueRangeState = new ValueRangeState<>(descriptor, Objects.requireNonNull(cachedWorkingSolution));
            }
            return (ValueRangeState<Solution_, Entity_, Value_>) singleValueRangeState;
        } else {
            var descriptorState = multipleValueRangeState[descriptor.getOrdinal()];
            if (descriptorState == null) {
                descriptorState = new ValueRangeState<>(descriptor, Objects.requireNonNull(cachedWorkingSolution));
                multipleValueRangeState[descriptor.getOrdinal()] = descriptorState;
            }
            return (ValueRangeState<Solution_, Entity_, Value_>) descriptorState;
        }
    }

    /**
     * As defined by {@link #getFromSolution(AbstractValueRangeDescriptor, Object)},
     * but the solution is taken from the cached working solution.
     * This requires {@link #reset(Object)} to be called before the first call to this method,
     * and therefore this method will throw an exception if called before the score director is instantiated.
     *
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    public <Value_> ValueRange<Value_> getFromSolution(AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException("Impossible state: value range (%s) requested before the working solution is known."
                    .formatted(valueRangeDescriptor));
        }
        return getFromSolution(valueRangeDescriptor, cachedWorkingSolution);
    }

    public <Value_> ValueRange<Value_> getFromSolution(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException("Impossible state: value range (%s) requested before the working solution is known."
                    .formatted(abstractValueRangeDescriptor));
        }
        return getFromSolution(abstractValueRangeDescriptor, cachedWorkingSolution, sorter);
    }

    public <Value_> ValueRange<Value_> getFromSolution(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor,
            Solution_ solution) {
        return getFromSolution(abstractValueRangeDescriptor, solution, null);
    }

    public <Value_> ValueRange<Value_> getFromSolution(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor,
            Solution_ solution, @Nullable SelectionSorter<Solution_, Value_> sorter) {
        ValueRangeState<Solution_, ?, Value_> descriptor = fromDescriptor(abstractValueRangeDescriptor);
        return descriptor.getFromSolution(solution, sorter);
    }

    /**
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    public <Entity_, Value_> ValueRange<Value_>
            getFromEntity(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor, Entity_ entity) {
        return getFromEntity(abstractValueRangeDescriptor, entity, null);
    }

    /**
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    public <Entity_, Value_> ValueRange<Value_> getFromEntity(
            AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor, Entity_ entity,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        ValueRangeState<Solution_, Entity_, Value_> descriptor = fromDescriptor(abstractValueRangeDescriptor);
        return descriptor.getFromEntity(entity, getInitializationStatistics().genuineEntityCount(), sorter);
    }

    public long countOnSolution(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor, Solution_ solution) {
        return getFromSolution(abstractValueRangeDescriptor, solution).getSize();
    }

    public <Entity_> long countOnEntity(AbstractValueRangeDescriptor<Solution_> abstractValueRangeDescriptor, Entity_ entity) {
        return getFromEntity(abstractValueRangeDescriptor, entity).getSize();
    }

    public <Entity_, Value_> ReachableValues<Entity_, Value_>
            getReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor) {
        return getReachableValues(variableDescriptor, null);
    }

    public <Entity_, Value_> ReachableValues<Entity_, Value_> getReachableValues(
            GenuineVariableDescriptor<Solution_> variableDescriptor, @Nullable SelectionSorter<Solution_, Value_> sorter) {
        ValueRangeState<Solution_, Entity_, Value_> descriptor = fromDescriptor(variableDescriptor.getValueRangeDescriptor());
        return descriptor.getReachableValues(variableDescriptor, sorter);
    }

    public void reset(@Nullable Solution_ workingSolution) {
        singleValueRangeState = null;
        if (multipleValueRangeState != null) {
            Arrays.fill(multipleValueRangeState, null);
        }
        // We only update the cached solution if it is not null; null means to only reset the maps.
        if (workingSolution != null) {
            cachedWorkingSolution = workingSolution;
            statistics = null;
        }
    }

}
