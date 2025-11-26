package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.ProblemScaleTracker;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.primdouble.DoubleValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SelectionSorterAdapter;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableItemValue;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.impl.util.MutableLong;

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

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final @Nullable ValueRangeItem<Solution_, CountableValueRange<?>>[] fromSolution;
    private final @Nullable Map<Object, Integer>[] fromSolutionValueIndexMap;
    private final Map<Object, ValueRangeItem<Solution_, CountableValueRange<?>>[]> fromEntityMap = new IdentityHashMap<>();
    private final @Nullable ValueRangeItem<Solution_, ReachableValues>[] reachableValues;
    private final @Nullable Map<BitSetItem, Object>[] fromEntityBitSet;

    private @Nullable Solution_ cachedWorkingSolution = null;
    private @Nullable SolutionInitializationStatistics cachedInitializationStatistics = null;
    private @Nullable ProblemSizeStatistics cachedProblemSizeStatistics = null;

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
        this.fromSolution = new ValueRangeItem[solutionDescriptor.getValueRangeDescriptorCount()];
        this.fromSolutionValueIndexMap = new Map[solutionDescriptor.getValueRangeDescriptorCount()];
        this.reachableValues = new ValueRangeItem[solutionDescriptor.getValueRangeDescriptorCount()];
        this.fromEntityBitSet = new HashMap[solutionDescriptor.getValueRangeDescriptorCount()];
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
        if (finisher == null) {
            if (cachedInitializationStatistics == null) {
                cachedInitializationStatistics = computeInitializationStatistics(cachedWorkingSolution, null);
            }
        } else {
            // If a finisher is provided, we always recompute the statistics,
            // because the finisher is expected to be called for every entity.
            cachedInitializationStatistics = computeInitializationStatistics(cachedWorkingSolution, finisher);
        }
        return cachedInitializationStatistics;
    }

    public SolutionInitializationStatistics computeInitializationStatistics(Solution_ solution,
            @Nullable Consumer<Object> finisher) {
        /*
         * The score director requires all of these data points,
         * so we calculate them all in a single pass over the entities.
         * This is an important performance improvement,
         * as there are potentially thousands of entities.
         */
        var uninitializedEntityCount = new MutableInt();
        var uninitializedVariableCount = new MutableInt();
        var unassignedValueCount = new MutableInt();
        var notInAnyListValueCount = new MutableInt();
        var genuineEntityCount = new MutableInt();
        var shadowEntityCount = new MutableInt();

        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        if (listVariableDescriptor != null) {
            var countOnSolution = (int) countOnSolution(listVariableDescriptor.getValueRangeDescriptor(), solution);
            notInAnyListValueCount.add(countOnSolution);
            if (!listVariableDescriptor.allowsUnassignedValues()) {
                // We count every possibly unassigned element in every list variable.
                // And later we subtract the assigned elements.
                unassignedValueCount.add(countOnSolution);
            }
        }

        solutionDescriptor.visitAllEntities(solution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineEntityCount.increment();
                var uninitializedVariableCountForEntity = entityDescriptor.countUninitializedVariables(entity);
                if (uninitializedVariableCountForEntity > 0) {
                    uninitializedEntityCount.increment();
                    uninitializedVariableCount.add(uninitializedVariableCountForEntity);
                }
            } else {
                shadowEntityCount.increment();
            }
            if (finisher != null) {
                finisher.accept(entity);
            }
            if (!entityDescriptor.hasAnyGenuineListVariables()) {
                return;
            }
            var listVariableEntityDescriptor = listVariableDescriptor.getEntityDescriptor();
            var countOnEntity = listVariableDescriptor.getListSize(entity);
            notInAnyListValueCount.subtract(countOnEntity);
            if (!listVariableDescriptor.allowsUnassignedValues() && listVariableEntityDescriptor.matchesEntity(entity)) {
                unassignedValueCount.subtract(countOnEntity);
            }
            // TODO maybe detect duplicates and elements that are outside the value range
        });
        return new SolutionInitializationStatistics(genuineEntityCount.intValue(),
                shadowEntityCount.intValue(),
                uninitializedEntityCount.intValue(), uninitializedVariableCount.intValue(), unassignedValueCount.intValue(),
                notInAnyListValueCount.intValue());
    }

    public ProblemSizeStatistics getProblemSizeStatistics() {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException("Impossible state: problem size requested before the working solution is known.");
        } else if (cachedProblemSizeStatistics == null) {
            cachedProblemSizeStatistics = new ProblemSizeStatistics(
                    solutionDescriptor.getGenuineEntityCount(cachedWorkingSolution),
                    solutionDescriptor.getGenuineVariableCount(cachedWorkingSolution),
                    getApproximateValueCount(),
                    getProblemScale());
        }
        return cachedProblemSizeStatistics;
    }

    long getApproximateValueCount() {
        var genuineVariableDescriptorSet =
                Collections.newSetFromMap(new IdentityHashMap<GenuineVariableDescriptor<Solution_>, Boolean>());
        solutionDescriptor.visitAllEntities(cachedWorkingSolution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                genuineVariableDescriptorSet.addAll(entityDescriptor.getGenuineVariableDescriptorList());
            }
        });
        var out = new MutableLong();
        for (var variableDescriptor : genuineVariableDescriptorSet) {
            var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
            if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
                out.add(countOnSolution(valueRangeDescriptor, cachedWorkingSolution));
            } else {
                solutionDescriptor.visitEntitiesByEntityClass(cachedWorkingSolution,
                        variableDescriptor.getEntityDescriptor().getEntityClass(),
                        entity -> {
                            out.add(countOnEntity(valueRangeDescriptor, entity));
                            return false;
                        });
            }
        }
        return out.longValue();
    }

    /**
     * Calculates an indication of the size of the problem instance.
     * This is approximately the base 10 logarithm of the search space size.
     *
     * <p>
     * The method uses a logarithmic scale to estimate the problem size,
     * where the base of the logarithm is determined by the maximum value range size.
     * It accounts for both basic variables and list variables in the solution,
     * considering pinned values and value ranges on both entity and solution.
     *
     * @return A non-negative double value representing the approximate base 10 logarithm of the search space size.
     *         Returns {@code 0} if the calculation results in NaN or infinity.
     */
    double getProblemScale() {
        var logBase = Math.max(2, getMaximumValueRangeSize());
        var problemScaleTracker = new ProblemScaleTracker(logBase);
        solutionDescriptor.visitAllEntities(cachedWorkingSolution, entity -> {
            var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
            if (entityDescriptor.isGenuine()) {
                processProblemScale(entityDescriptor, entity, problemScaleTracker);
            }
        });
        var result = problemScaleTracker.getBasicProblemScaleLog();
        if (problemScaleTracker.getListTotalEntityCount() != 0L) {
            // List variables do not support from entity value ranges
            var totalListValueCount = problemScaleTracker.getListTotalValueCount();
            var totalListMovableValueCount = totalListValueCount - problemScaleTracker.getListPinnedValueCount();
            var possibleTargetsForListValue = problemScaleTracker.getListMovableEntityCount();
            var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
            if (listVariableDescriptor != null && listVariableDescriptor.allowsUnassignedValues()) {
                // Treat unassigned values as assigned to a single virtual vehicle for the sake of this calculation
                possibleTargetsForListValue++;
            }

            result += MathUtils.getPossibleArrangementsScaledApproximateLog(MathUtils.LOG_PRECISION, logBase,
                    totalListMovableValueCount, possibleTargetsForListValue);
        }
        var scale = (result / (double) MathUtils.LOG_PRECISION) / MathUtils.getLogInBase(logBase, 10d);
        if (Double.isNaN(scale) || Double.isInfinite(scale)) {
            return 0;
        }
        return scale;
    }

    /**
     * Calculates the maximum value range size across all entities in the working solution.
     * <p>
     * The "maximum value range size" is defined as the largest number of possible values
     * for any genuine variable across all entities.
     * This is determined by inspecting the value range descriptors of each variable in each entity.
     *
     * @return The maximum value range size, or 0 if no genuine variables are found.
     */
    long getMaximumValueRangeSize() {
        return solutionDescriptor.extractAllEntitiesStream(cachedWorkingSolution)
                .mapToLong(entity -> {
                    var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entity.getClass());
                    return entityDescriptor.isGenuine()
                            ? getMaximumValueCount(entityDescriptor, entity)
                            : 0L;
                })
                .max()
                .orElse(0L);
    }

    private long getMaximumValueCount(EntityDescriptor<Solution_> entityDescriptor, Object entity) {
        var maximumValueCount = 0L;
        for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
            if (variableDescriptor.canExtractValueRangeFromSolution()) {
                maximumValueCount = Math.max(maximumValueCount,
                        countOnSolution(variableDescriptor.getValueRangeDescriptor(), cachedWorkingSolution));
            } else {
                maximumValueCount =
                        Math.max(maximumValueCount, countOnEntity(variableDescriptor.getValueRangeDescriptor(), entity));
            }
        }
        return maximumValueCount;

    }

    private void processProblemScale(EntityDescriptor<Solution_> entityDescriptor, Object entity, ProblemScaleTracker tracker) {
        for (var variableDescriptor : entityDescriptor.getGenuineVariableDescriptorList()) {
            var valueCount = variableDescriptor.canExtractValueRangeFromSolution()
                    ? countOnSolution(variableDescriptor.getValueRangeDescriptor(), cachedWorkingSolution)
                    : countOnEntity(variableDescriptor.getValueRangeDescriptor(), entity);
            // TODO: When minimum Java supported is 21, this can be replaced with a sealed interface switch
            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor) {
                if (basicVariableDescriptor.isChained()) {
                    // An entity is a value
                    tracker.addListValueCount(1);
                    if (!entityDescriptor.isMovable(cachedWorkingSolution, entity)) {
                        tracker.addPinnedListValueCount(1);
                    }
                    // Anchors are entities
                    var valueRange = variableDescriptor.canExtractValueRangeFromSolution()
                            ? getFromSolution(variableDescriptor.getValueRangeDescriptor(), cachedWorkingSolution)
                            : getFromEntity(variableDescriptor.getValueRangeDescriptor(), entity);
                    var valueIterator = valueRange.createOriginalIterator();
                    while (valueIterator.hasNext()) {
                        var value = valueIterator.next();
                        if (variableDescriptor.isValuePotentialAnchor(value)) {
                            if (tracker.isAnchorVisited(value)) {
                                continue;
                            }
                            // Assumes anchors are not pinned
                            tracker.incrementListEntityCount(true);
                        }
                    }
                } else {
                    if (entityDescriptor.isMovable(cachedWorkingSolution, entity)) {
                        tracker.addBasicProblemScale(valueCount);
                    }
                }
            } else if (variableDescriptor instanceof ListVariableDescriptor<Solution_> listVariableDescriptor) {
                var size = countOnSolution(listVariableDescriptor.getValueRangeDescriptor(), cachedWorkingSolution);
                tracker.setListTotalValueCount((int) size);
                if (entityDescriptor.isMovable(cachedWorkingSolution, entity)) {
                    tracker.incrementListEntityCount(true);
                    tracker.addPinnedListValueCount(listVariableDescriptor.getFirstUnpinnedIndex(entity));
                } else {
                    tracker.incrementListEntityCount(false);
                    tracker.addPinnedListValueCount(listVariableDescriptor.getListSize(entity));
                }
            } else {
                throw new IllegalStateException(
                        "Unhandled subclass of %s encountered (%s).".formatted(VariableDescriptor.class.getSimpleName(),
                                variableDescriptor.getClass().getSimpleName()));
            }
        }
    }

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

    public <T> CountableValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            @Nullable SelectionSorter<Solution_, T> sorter) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: value range (%s) requested before the working solution is known."
                            .formatted(valueRangeDescriptor));
        }
        return getFromSolution(valueRangeDescriptor, cachedWorkingSolution, sorter);
    }

    @SuppressWarnings("unchecked")
    public <T> CountableValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Solution_ solution) {
        return getFromSolution(valueRangeDescriptor, solution, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> CountableValueRange<T> getFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ solution,
            @Nullable SelectionSorter<Solution_, T> sorter) {
        var item = fromSolution[valueRangeDescriptor.getOrdinal()];
        // No item, we set the left side by default
        if (item == null) {
            var valueRange = fetchValueRangeFromSolution(valueRangeDescriptor, solution, sorter);
            fromSolution[valueRangeDescriptor.getOrdinal()] = new ValueRangeItem<>(null, valueRange, sorter, null, null);
            updateSolutionIndexMap(valueRangeDescriptor, valueRange);
            return valueRange;
        }
        var valueRange = pickValueRangeBySorter(item, sorter, null);
        if (valueRange != null) {
            return valueRange;
        }
        // If the left sorter is null, and the given sorter is not, we replace the left value with a sorted one.
        if (item.leftSorter() == null && sorter != null) {
            if (!(item.leftItem() instanceof SortableValueRange sortableValueRange)) {
                throw new IllegalStateException(
                        "Impossible state: value range (%s) on planning solution (%s) is not sortable."
                                .formatted(valueRangeDescriptor, solution));
            }
            var sorterAdapter = SelectionSorterAdapter.of(solution, sorter);
            var newItem = new ValueRangeItem<>(null, (CountableValueRange<T>) sortableValueRange.sort(sorterAdapter), sorter,
                    item.rightItem(), item.rightSorter());
            fromSolution[valueRangeDescriptor.getOrdinal()] = newItem;
            var sortedValueRange = (CountableValueRange<T>) newItem.leftItem();
            // We need to update the index map or the positions may become inconsistent
            updateSolutionIndexMap(valueRangeDescriptor, Objects.requireNonNull(sortedValueRange));
            return sortedValueRange;
        } else if (item.rightItem() == null) {
            var newItem = new ValueRangeItem<>(null, item.leftItem(), item.leftSorter(),
                    fetchValueRangeFromSolution(valueRangeDescriptor, solution, sorter), sorter);
            fromSolution[valueRangeDescriptor.getOrdinal()] = newItem;
            return (CountableValueRange<T>) Objects.requireNonNull(newItem.rightItem());
        } else {
            throw new IllegalStateException(
                    "Impossible state: the value range (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(valueRangeDescriptor, sorter, item.leftItem(), item.rightItem()));
        }
    }

    private <T> void updateSolutionIndexMap(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            CountableValueRange<T> valueRange) {
        fromSolutionValueIndexMap[valueRangeDescriptor.getOrdinal()] =
                (Map<Object, Integer>) buildIndexMap(valueRange.createOriginalIterator(), (int) valueRange.getSize(),
                        ConfigUtils.isGenericTypeImmutable(findValueClass(valueRange)));
    }

    private Map<Object, Integer> getIndexMapFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        var indexMap = fromSolutionValueIndexMap[valueRangeDescriptor.getOrdinal()];
        if (indexMap == null) {
            // We call getFromSolution to ensure the solution-range is loaded and the related index map is created
            getFromSolution(valueRangeDescriptor);
            indexMap = Objects.requireNonNull(fromSolutionValueIndexMap[valueRangeDescriptor.getOrdinal()]);
        }
        return indexMap;
    }

    private <T> CountableValueRange<T> fetchValueRangeFromSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Solution_ solution, @Nullable SelectionSorter<Solution_, T> sorter) {
        CountableValueRange<T> valueRange = extractValueRange(valueRangeDescriptor, solution);
        if (sorter != null) {
            if (!(valueRange instanceof SortableValueRange sortableValueRange)) {
                throw new IllegalStateException(
                        "Impossible state: value range (%s) on planning solution (%s) is not sortable."
                                .formatted(valueRangeDescriptor, solution));
            }
            var sorterAdapter = SelectionSorterAdapter.of(solution, sorter);
            return (CountableValueRange<T>) sortableValueRange.sort(sorterAdapter);
        }
        return valueRange;
    }

    private <T> CountableValueRange<T> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Solution_ solution) {
        var extractedValueRange = valueRangeDescriptor.<T> extractAllValues(Objects.requireNonNull(solution));
        if (!(extractedValueRange instanceof CountableValueRange<T> countableValueRange)) {
            throw new UnsupportedOperationException("""
                    Impossible state: value range (%s) on planning solution (%s) is not countable.
                    Maybe replace %s with %s."""
                    .formatted(valueRangeDescriptor, solution, DoubleValueRange.class.getSimpleName(),
                            BigDecimalValueRange.class.getSimpleName()));
        } else if (valueRangeDescriptor.acceptsNullInValueRange()) {
            return new NullAllowingCountableValueRange<>(countableValueRange);
        } else {
            return countableValueRange;
        }
    }

    /**
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    @SuppressWarnings("unchecked")
    public <T> CountableValueRange<T> getFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        return getFromEntity(valueRangeDescriptor, entity, null);
    }

    /**
     * @throws IllegalStateException if called before {@link #reset(Object)} is called
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> CountableValueRange<T> getFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity,
            @Nullable SelectionSorter<Solution_, T> sorter) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: value range (%s) on planning entity (%s) requested before the working solution is known."
                            .formatted(valueRangeDescriptor, entity));
        }
        var valueRangeList = fromEntityMap.computeIfAbsent(entity,
                e -> new ValueRangeItem[solutionDescriptor.getValueRangeDescriptorCount()]);
        var item = valueRangeList[valueRangeDescriptor.getOrdinal()];
        // No item, we set the left side by default
        if (item == null) {
            var newItem = buildEntityValueRangeItem(valueRangeDescriptor, entity, sorter, true);
            valueRangeList[valueRangeDescriptor.getOrdinal()] = newItem;
            if (newItem.entity() != null && newItem.leftItem() == null && newItem.rightItem() == null) {
                // Placeholder for another entity
                return getFromEntity(valueRangeDescriptor, Objects.requireNonNull(newItem.entity()), sorter);
            }
            return (CountableValueRange<T>) Objects.requireNonNull(newItem.leftItem());
        }
        var valueRange =
                pickValueRangeBySorter(item, sorter, placeholder -> getFromEntity(valueRangeDescriptor, placeholder, sorter));
        if (valueRange != null) {
            return valueRange;
        }
        if (item.leftSorter() == null) {
            // The current left sorter is null and need to be updated
            var newItem = buildEntityValueRangeItem(valueRangeDescriptor, entity, sorter, false);
            valueRangeList[valueRangeDescriptor.getOrdinal()] = new ValueRangeItem<>(entity,
                    // Left item from the newly created item
                    newItem.leftItem(),
                    newItem.leftSorter(),
                    // Current right item
                    item.rightItem(),
                    item.rightSorter());
            return (CountableValueRange<T>) Objects.requireNonNull(newItem.leftItem());
        } else if (item.rightItem() == null) {
            // The new item stores the sorted right item at the left position
            var newItem = buildEntityValueRangeItem(valueRangeDescriptor, entity, sorter, false);
            valueRangeList[valueRangeDescriptor.getOrdinal()] = new ValueRangeItem<>(entity,
                    // Current left item
                    item.leftItem(),
                    item.leftSorter(),
                    // Right item from the newly created item
                    newItem.leftItem(),
                    newItem.leftSorter());
            return (CountableValueRange<T>) Objects.requireNonNull(newItem.leftItem());
        } else {
            throw new IllegalStateException(
                    "Impossible state: the value range (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(valueRangeDescriptor, sorter, item.leftSorter(), item.rightSorter()));
        }
    }

    private <T> ValueRangeItem<Solution_, CountableValueRange<?>> buildEntityValueRangeItem(
            ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity, @Nullable SelectionSorter<Solution_, T> sorter,
            boolean checkEntityMatch) {
        var valueRange = fetchValueRangeFromEntity(valueRangeDescriptor, entity, sorter);
        if (checkEntityMatch) {
            var entityMatch = findEntityBitSetMatch(valueRangeDescriptor, entity,
                    getInitializationStatistics().genuineEntityCount(), valueRange);
            if (entityMatch != null) {
                return new ValueRangeItem<>(entityMatch, null, null, null, null);
            }
        }
        // We set only the left side
        return new ValueRangeItem<>(entity, valueRange, sorter, null, null);
    }

    private <T> @Nullable CountableValueRange<T> pickValueRangeBySorter(ValueRangeItem<Solution_, CountableValueRange<?>> item,
            @Nullable SelectionSorter<Solution_, T> sorter,
            @Nullable Function<Object, CountableValueRange<T>> placeholderFunction) {
        // We verify whether it serves as a placeholder to another value range
        if (item.leftItem() == null && item.rightItem() == null && placeholderFunction != null) {
            var placeholder = item.entity();
            if (placeholder == null) {
                throw new IllegalStateException("Impossible state: the placeholder is null and no value ranges are found.");
            }
            return placeholderFunction.apply(placeholder);
        } else if (item.leftItem() != null && (sorter == null || Objects.equals(item.leftSorter(), sorter))) {
            // Return the left value if there is no sorter or if the left sorter is the same as the provided one.
            return (CountableValueRange<T>) item.leftItem();
        } else if (item.rightItem() != null && Objects.equals(item.rightSorter(), sorter)) {
            // Return the right value only if the right sorter is the same as the provided one.
            return (CountableValueRange<T>) item.rightItem();
        }
        return null;
    }

    /**
     * Search for an identical bitset linked to another entity.
     * The goal is to deduplicate the value ranges stored in memory.
     *
     * @param valueRangeDescriptor the entity value range descriptor
     * @param entity the entity
     * @param valueRange the entity value range
     * @return returns an existing entity with a matching value range, or returns null if it does not exist.
     */
    private @Nullable <T> Object findEntityBitSetMatch(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity,
            int entitySize, CountableValueRange<T> valueRange) {
        // We create a BitSet from the range values
        // and check if another identical range already exists to prevent duplication
        var valueIndexMap = getIndexMapFromSolution(valueRangeDescriptor);
        var valueRangeBitSet = getBitSetValueRange(valueRange, valueIndexMap);
        var fromEntity = findEntityBitSetMap(valueRangeDescriptor, entity, entitySize, valueRange, valueRangeBitSet);
        if (fromEntity == null) {
            return null;
        }
        var match = fromEntity.get(BitSetItem.of(valueRange, valueRangeBitSet));
        if (match == null) {
            fromEntity.put(BitSetItem.of(valueRange, valueRangeBitSet), entity);
        }
        return match;
    }

    private <T> @Nullable Map<BitSetItem, Object> findEntityBitSetMap(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Object entity,
            int entitySize, CountableValueRange<T> valueRange, BitSet valueRangeBitSet) {
        var fromEntity = fromEntityBitSet[valueRangeDescriptor.getOrdinal()];
        if (fromEntity == null) {
            fromEntity = new HashMap<>(entitySize);
            fromEntityBitSet[valueRangeDescriptor.getOrdinal()] = fromEntity;
            fromEntity.put(BitSetItem.of(valueRange, valueRangeBitSet), entity);
            return null;
        }
        return fromEntity;
    }

    private <T> CountableValueRange<T> fetchValueRangeFromEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Object entity, @Nullable SelectionSorter<Solution_, T> sorter) {
        CountableValueRange<T> valueRange;
        var extractedValueRange =
                valueRangeDescriptor.<T> extractValuesFromEntity(Objects.requireNonNull(cachedWorkingSolution),
                        Objects.requireNonNull(entity));
        if (!(extractedValueRange instanceof CountableValueRange<T> countableValueRange)) {
            throw new UnsupportedOperationException("""
                    Impossible state: value range (%s) on planning entity (%s) is not countable.
                    Maybe replace %s with %s."""
                    .formatted(valueRangeDescriptor, entity, DoubleValueRange.class.getSimpleName(),
                            BigDecimalValueRange.class.getSimpleName()));
        } else if (valueRangeDescriptor.acceptsNullInValueRange()) {
            valueRange = new NullAllowingCountableValueRange<>(countableValueRange);
        } else {
            valueRange = countableValueRange;
        }
        if (sorter != null) {
            if (!(valueRange instanceof SortableValueRange sortableValueRange)) {
                throw new IllegalStateException(
                        "Impossible state: value range (%s) on planning entity (%s) is not sortable."
                                .formatted(valueRangeDescriptor, entity));
            }
            var sorterAdapter = SelectionSorterAdapter.of(cachedWorkingSolution, sorter);
            valueRange = (CountableValueRange<T>) sortableValueRange.sort(sorterAdapter);
        }
        return valueRange;
    }

    private static <T> BitSet getBitSetValueRange(CountableValueRange<T> valueRange, Map<Object, Integer> valueIndexMap) {
        var valueBitSet = new BitSet((int) valueRange.getSize());
        var iterator = valueRange.createOriginalIterator();
        while (iterator.hasNext()) {
            var value = iterator.next();
            if (value == null) {
                continue;
            }
            valueBitSet.set(valueIndexMap.get(value));
        }
        return valueBitSet;
    }

    public long countOnSolution(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ solution) {
        return getFromSolution(valueRangeDescriptor, solution)
                .getSize();
    }

    public long countOnEntity(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Object entity) {
        return getFromEntity(valueRangeDescriptor, entity)
                .getSize();
    }

    public ReachableValues getReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor) {
        return getReachableValues(variableDescriptor, null);
    }

    public ReachableValues getReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        if (cachedWorkingSolution == null) {
            throw new IllegalStateException(
                    "Impossible state: value reachability requested before the working solution is known.");
        }
        var item = reachableValues[variableDescriptor.getValueRangeDescriptor().getOrdinal()];
        // No item, we set the leftItem side by default
        if (item == null) {
            var values = fetchReachableValues(variableDescriptor, sorter);
            reachableValues[variableDescriptor.getValueRangeDescriptor().getOrdinal()] =
                    new ValueRangeItem<>(null, values, sorter, null, null);
            return values;
        }
        var value = pickReachableValuesBySorter(item, sorter);
        if (value != null) {
            return value;
        }
        // If the left sorter is null, and the given sorter is not, we replace the left value with a sorted one.
        if (item.leftItem() != null && item.leftSorter() == null && sorter != null) {
            // We clear the left item as we are copying it and updating the sorter
            var sortedValues = buildSortedReachableValues(item.leftItem(), sorter, true);
            reachableValues[variableDescriptor.getValueRangeDescriptor().getOrdinal()] =
                    new ValueRangeItem<>(null,
                            // Left item from the newly sorted item
                            sortedValues,
                            sorter,
                            // Current right item
                            item.rightItem(),
                            item.rightSorter());
            return sortedValues;
        } else if (item.rightItem() == null && sorter != null) {
            // We use the existing left item to create a new structure with a different sorter
            var sortedValues = buildSortedReachableValues(item.leftItem(), sorter, false);
            reachableValues[variableDescriptor.getValueRangeDescriptor().getOrdinal()] =
                    new ValueRangeItem<>(null,
                            // Current left item
                            item.leftItem(),
                            item.leftSorter(),
                            // Right item from the newly sorted item
                            sortedValues,
                            sorter);
            return sortedValues;
        } else {
            throw new IllegalStateException(
                    "Impossible state: the reachable values structure for variable (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(variableDescriptor, sorter, item.leftSorter(), item.rightSorter()));
        }
    }

    private <T> @Nullable ReachableValues pickReachableValuesBySorter(ValueRangeItem<Solution_, ReachableValues> item,
            @Nullable SelectionSorter<Solution_, T> sorter) {
        if (item.leftItem() != null && (sorter == null || Objects.equals(item.leftSorter(), sorter))) {
            // Return the left value if there is no sorter or if the left sorter is the same as the provided one.
            return item.leftItem();
        } else if (item.rightItem() != null && Objects.equals(item.rightSorter(), sorter)) {
            // Return the right value only if the right sorter is the same as the provided one.
            return item.rightItem();
        }
        return null;
    }

    private ReachableValues buildSortedReachableValues(ReachableValues values, SelectionSorter<Solution_, ?> sorter,
            boolean clear) {
        var sorterAdapter = SelectionSorterAdapter.of(cachedWorkingSolution, sorter);
        var sortedValues = Objects.requireNonNull(values).copy(sorterAdapter);
        if (clear) {
            values.clear();
        }
        return sortedValues;
    }

    private ReachableValues fetchReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        ReachableValues values;
        var entityDescriptor = variableDescriptor.getEntityDescriptor();
        var entityList = entityDescriptor.extractEntities(cachedWorkingSolution);
        var entityIndexMap = buildIndexMap(entityList.iterator(), entityList.size(), false);
        var valueList = getFromSolution(variableDescriptor.getValueRangeDescriptor());
        var valueListSize = valueList.getSize();
        if (valueListSize > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "The structure %s cannot be built for the entity %s (%s) because value range has a size (%d) which is higher than Integer.MAX_VALUE."
                            .formatted(ReachableValues.class.getSimpleName(),
                                    entityDescriptor.getEntityClass().getSimpleName(),
                                    variableDescriptor.getVariableName(), valueListSize));
        }
        Class<?> valueClass = findValueClass(valueList);
        var valueIndexMap = getIndexMapFromSolution(variableDescriptor.getValueRangeDescriptor());
        var reachableValueList = initReachableValueList(valueList, entityList.size());
        for (var i = 0; i < entityList.size(); i++) {
            var entity = entityList.get(i);
            var valueRange = getFromEntity(variableDescriptor.getValueRangeDescriptor(), entity);
            loadEntityValueRange(i, valueIndexMap, valueRange, reachableValueList);
        }
        var sorterAdapter = sorter != null ? SelectionSorterAdapter.of(cachedWorkingSolution, sorter) : null;
        values = new ReachableValues(entityIndexMap, entityList, valueIndexMap, reachableValueList, valueClass, sorterAdapter,
                variableDescriptor.getValueRangeDescriptor().acceptsNullInValueRange());
        return values;
    }

    private static <T> @Nullable Class<?> findValueClass(CountableValueRange<T> valueRange) {
        Class<?> valueClass = null;
        var idx = 0;
        while (idx < valueRange.getSize()) {
            var value = valueRange.get(idx++);
            if (value == null) {
                continue;
            }
            valueClass = value.getClass();
            break;
        }
        return valueClass;
    }

    private static <T> Map<T, Integer> buildIndexMap(Iterator<@Nullable T> allValues, int size, boolean isImmutable) {
        Map<T, Integer> indexMap = isImmutable ? new HashMap<>(size) : new IdentityHashMap<>(size);
        var idx = 0;
        while (allValues.hasNext()) {
            var value = allValues.next();
            if (value == null) {
                continue;
            }
            indexMap.put(value, idx++);
        }
        return indexMap;
    }

    private static List<ReachableItemValue> initReachableValueList(CountableValueRange<Object> valueRange, int entityListSize) {
        var size = (int) valueRange.getSize();
        var valueList = new ArrayList<ReachableItemValue>(size);
        var idx = 0;
        for (var i = 0; i < size; i++) {
            var value = valueRange.get(i);
            if (value == null) {
                continue;
            }
            valueList.add(new ReachableItemValue(idx++, value, entityListSize, size));
        }
        return valueList;
    }

    private static <T> void loadEntityValueRange(int entityIndex, Map<Object, Integer> valueIndexMap,
            CountableValueRange<T> valueRange, List<ReachableItemValue> reachableValueList) {
        var allValuesBitSet = new BitSet(reachableValueList.size());
        // We create a bitset containing all possible values from the range to optimize operations
        for (var i = 0; i < valueRange.getSize(); i++) {
            var value = valueRange.get(i);
            if (value == null) {
                continue;
            }
            allValuesBitSet.set(valueIndexMap.get(value));
        }
        for (var i = 0; i < valueRange.getSize(); i++) {
            var value = valueRange.get(i);
            if (value == null) {
                continue;
            }
            var valueIndex = valueIndexMap.get(value);
            var item = reachableValueList.get(valueIndex);
            item.addEntity(entityIndex);
            // We unset the current value index to import only the values that are reachable
            allValuesBitSet.clear(valueIndex);
            item.addValues(allValuesBitSet);
            // We set it again to restore the state for the next value
            allValuesBitSet.set(valueIndex);
        }
    }

    public void reset(@Nullable Solution_ workingSolution) {
        Arrays.fill(fromSolution, null);
        Arrays.fill(fromSolutionValueIndexMap, null);
        Arrays.fill(reachableValues, null);
        Arrays.fill(fromEntityBitSet, null);
        fromEntityMap.clear();
        // We only update the cached solution if it is not null; null means to only reset the maps.
        if (workingSolution != null) {
            cachedWorkingSolution = workingSolution;
            cachedInitializationStatistics = null;
            cachedProblemSizeStatistics = null;
        }
    }

    private record ValueRangeItem<Solution_, V>(@Nullable Object entity, @Nullable V leftItem,
            @Nullable SelectionSorter<Solution_, ?> leftSorter, @Nullable V rightItem,
            @Nullable SelectionSorter<Solution_, ?> rightSorter) {

    }

    /**
     * The record holds a reference to {@link BitSet},
     * a precomputed hash to avoid recalculating it every time since the BitSet should be immutable.
     * It also includes the size of the BitSet to skip unnecessary comparisons when searching it in the map.
     */
    private record BitSetItem(int size, int hash, BitSet item) {

        public static <T> BitSetItem of(CountableValueRange<T> valueRange, BitSet item) {
            return new BitSetItem((int) valueRange.getSize(), item.hashCode(), item);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BitSetItem that))
                return false;
            return size() == that.size() && hash() == that.hash() && Objects.equals(item(), that.item());
        }
    }

}
