package ai.timefold.solver.core.impl.score.director;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.composite.NullAllowingCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.primdouble.DoubleValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SelectionSorterAdapter;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableItemValue;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableValuesIndex;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ValueRangeState<Solution_, Entity_, Value_> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final Solution_ cachedWorkingSolution;

    // Solution related fields
    private @Nullable ValueRangeItem<Solution_, Entity_, CountableValueRange<Value_>, Value_> fromSolutionItem;
    private @Nullable Map<Value_, Integer> fromSolutionValueIndexMap;

    // Entity related fields
    private @Nullable Map<Entity_, ValueRangeItem<Solution_, Entity_, CountableValueRange<Value_>, Value_>> fromEntityMap;
    private @Nullable Map<HashedValueRange<Value_>, Entity_> valueRangeDeduplicationCache;

    // ReachableValues related field
    private @Nullable ValueRangeItem<Solution_, Entity_, ReachableValues<Entity_, Value_>, Value_> reachableValuesItem;

    ValueRangeState(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ cachedWorkingSolution) {
        this.valueRangeDescriptor = Objects.requireNonNull(valueRangeDescriptor);
        this.cachedWorkingSolution = Objects.requireNonNull(cachedWorkingSolution);
    }

    public CountableValueRange<Value_> getFromSolution(Solution_ solution,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        // No item, we set the left side by default
        if (fromSolutionItem == null) {
            var valueRange = fetchValueRangeFromSolution(solution, sorter);
            fromSolutionItem = ValueRangeItem.ofLeft(null, valueRange, sorter);
            fromSolutionValueIndexMap = buildIndexMap(valueRange.createOriginalIterator(), (int) valueRange.getSize());
            return valueRange;
        }
        var valueRange = pickValueBySorter(fromSolutionItem, sorter, null);
        if (valueRange != null) {
            return valueRange;
        }
        // If the left sorter is null and the given sorter is not, we replace the left value with a sorted one.
        if (fromSolutionItem.leftSorter() == null && sorter != null) {
            var sortedValueRange = sortValueRange(Objects.requireNonNull(fromSolutionItem.leftItem()), sorter);
            fromSolutionItem =
                    ValueRangeItem.of(null, sortedValueRange, sorter, fromSolutionItem.rightItem(),
                            fromSolutionItem.rightSorter());
            // We need to update the index map or the positions may become inconsistent
            fromSolutionValueIndexMap = buildIndexMap(sortedValueRange.createOriginalIterator(),
                    (int) sortedValueRange.getSize());
            return sortedValueRange;
        } else if (fromSolutionItem.rightItem() == null) {
            var sortedValueRange = sortValueRange(Objects.requireNonNull(fromSolutionItem.leftItem()), sorter);
            fromSolutionItem =
                    ValueRangeItem.of(null, fromSolutionItem.leftItem(), fromSolutionItem.leftSorter(), sortedValueRange,
                            sorter);
            return sortedValueRange;
        } else {
            throw new IllegalStateException(
                    "Impossible state: the value range (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(valueRangeDescriptor, sorter, fromSolutionItem.leftItem(),
                                    fromSolutionItem.rightItem()));
        }
    }

    private <Type_> @Nullable Type_ pickValueBySorter(ValueRangeItem<Solution_, Entity_, Type_, Value_> item,
            @Nullable SelectionSorter<Solution_, Value_> sorter,
            @Nullable BiFunction<Entity_, @Nullable SelectionSorter<Solution_, Value_>, Type_> placeholderFunction) {
        // We verify whether it serves as a placeholder to another value range
        if (item.leftItem() == null && item.rightItem() == null && placeholderFunction != null) {
            var placeholder = item.entity();
            if (placeholder == null) {
                throw new IllegalStateException("Impossible state: the placeholder is null and no value ranges are found.");
            }
            return placeholderFunction.apply(placeholder, sorter);
        } else if (item.leftItem() != null && (sorter == null || Objects.equals(item.leftSorter(), sorter))) {
            // Return the left value if there is no sorter or if the left sorter is the same as the provided one.
            return item.leftItem();
        } else if (item.rightItem() != null && Objects.equals(item.rightSorter(), sorter)) {
            // Return the right value only if the right sorter is the same as the provided one.
            return item.rightItem();
        }
        return null;
    }

    private Map<Value_, Integer> getIndexMapFromSolution() {
        if (fromSolutionValueIndexMap == null) {
            // We call getFromSolution to ensure the solution-range is loaded and the related index map is created
            getFromSolution(cachedWorkingSolution, null);
        }
        return fromSolutionValueIndexMap;
    }

    private CountableValueRange<Value_> fetchValueRangeFromSolution(Solution_ solution,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var valueRange = extractValueRange(valueRangeDescriptor, solution);
        return sortValueRange(valueRange, sorter);
    }

    private CountableValueRange<Value_> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            Solution_ solution) {
        var extractedValueRange = valueRangeDescriptor.<Value_> extractAllValues(Objects.requireNonNull(solution));
        if (!(extractedValueRange instanceof CountableValueRange<Value_> countableValueRange)) {
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

    private CountableValueRange<Value_> sortValueRange(CountableValueRange<Value_> originalValueRange,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        if (sorter == null) {
            return originalValueRange;
        }
        if (!(originalValueRange instanceof SortableValueRange sortableValueRange)) {
            throw new IllegalStateException("Impossible state: value range (%s) on planning solution (%s) is not sortable."
                    .formatted(valueRangeDescriptor, cachedWorkingSolution));
        }
        return (CountableValueRange<Value_>) sortableValueRange.sort(SelectionSorterAdapter.of(cachedWorkingSolution, sorter));
    }

    public CountableValueRange<Value_> getFromEntity(Entity_ entity, int entityCount,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var entityMap = ensureEntityMapIsInitialized(entityCount);
        var item = entityMap.get(entity);
        // No item, we set the left side by default
        if (item == null) {
            var newItem = buildEntityValueRangeItem(entity, sorter);
            entityMap.put(entity, newItem);
            if (newItem.entity() != null && newItem.leftItem() == null && newItem.rightItem() == null) {
                // Placeholder for another entity
                return getFromEntity(Objects.requireNonNull(newItem.entity()), entityCount, sorter);
            }
            return Objects.requireNonNull(newItem.leftItem());
        }
        var valueRange =
                pickValueBySorter(item, sorter, (p, s) -> getFromEntity(p, entityCount, s));
        if (valueRange != null) {
            return valueRange;
        }
        if (item.leftSorter() == null && sorter != null) {
            // The current left sorter is null and need to be updated
            var newItem = ValueRangeItem.of(entity, sortValueRange(Objects.requireNonNull(item.leftItem()), sorter), sorter,
                    item.rightItem(), item.rightSorter());
            entityMap.put(entity, newItem);
            return Objects.requireNonNull(newItem.leftItem());
        } else if (item.rightItem() == null) {
            // The new item stores the sorted right item at the left position
            var newItem = ValueRangeItem.of(entity, item.leftItem(), item.leftSorter(),
                    sortValueRange(Objects.requireNonNull(item.leftItem()), sorter), sorter);
            entityMap.put(entity, newItem);
            return Objects.requireNonNull(newItem.rightItem());
        } else {
            throw new IllegalStateException(
                    "Impossible state: the value range (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(valueRangeDescriptor, sorter, item.leftSorter(), item.rightSorter()));
        }
    }

    private Map<Entity_, ValueRangeItem<Solution_, Entity_, CountableValueRange<Value_>, Value_>>
            ensureEntityMapIsInitialized(int entityCount) {
        if (fromEntityMap == null) {
            fromEntityMap = CollectionUtils.newIdentityHashMap(entityCount);
            valueRangeDeduplicationCache = CollectionUtils.newHashMap(entityCount);
        }
        return fromEntityMap;
    }

    private ValueRangeItem<Solution_, Entity_, CountableValueRange<Value_>, Value_> buildEntityValueRangeItem(Entity_ entity,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var valueRange = fetchValueRangeFromEntity(entity, sorter);
        var entityMatch = findEntityMatch(entity, valueRange);
        if (entityMatch != null) {
            return ValueRangeItem.ofEntity(entityMatch);
        }
        return ValueRangeItem.ofLeft(entity, valueRange, sorter);
    }

    /**
     * Search for an identical bitset linked to another entity.
     * The goal is to deduplicate the value ranges stored in memory.
     *
     * @param entity the entity
     * @param valueRange the entity value range
     * @return returns an existing entity with a matching value range, or returns null if it does not exist.
     */
    private @Nullable Entity_ findEntityMatch(Entity_ entity, CountableValueRange<Value_> valueRange) {
        var hashedValueRange = HashedValueRange.of(valueRange);
        var fromEntity = valueRangeDeduplicationCache.get(hashedValueRange);
        if (fromEntity == null) {
            valueRangeDeduplicationCache.put(hashedValueRange, entity);
            return null;
        }
        return fromEntity;
    }

    private CountableValueRange<Value_> fetchValueRangeFromEntity(Entity_ entity,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        CountableValueRange<Value_> valueRange;
        var extractedValueRange =
                valueRangeDescriptor.<Value_> extractValuesFromEntity(cachedWorkingSolution, Objects.requireNonNull(entity));
        if (!(extractedValueRange instanceof CountableValueRange<Value_> countableValueRange)) {
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
        return sortValueRange(valueRange, sorter);
    }

    public ReachableValues<Entity_, Value_> getReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        // No item, we set the leftItem side by default
        if (reachableValuesItem == null) {
            var values = fetchReachableValues(variableDescriptor, sorter);
            reachableValuesItem = ValueRangeItem.ofLeft(null, values, sorter);
            return values;
        }
        var value = pickValueBySorter(reachableValuesItem, sorter, null);
        if (value != null) {
            return value;
        }
        // If the left sorter is null and the given sorter is not, we replace the left value with a sorted one.
        if (reachableValuesItem.leftSorter() == null && sorter != null) {
            // We clear the left item as we are copying it and updating the sorter
            var sortedValues = buildSortedReachableValues(Objects.requireNonNull(reachableValuesItem.leftItem()), sorter, true);
            reachableValuesItem =
                    ValueRangeItem.of(null, sortedValues, sorter, reachableValuesItem.rightItem(),
                            reachableValuesItem.rightSorter());
            return sortedValues;
        } else if (reachableValuesItem.rightItem() == null && sorter != null) {
            // We use the existing left item to create a new structure with a different sorter
            var sortedValues =
                    buildSortedReachableValues(Objects.requireNonNull(reachableValuesItem.leftItem()), sorter, false);
            reachableValuesItem =
                    ValueRangeItem.of(null, Objects.requireNonNull(reachableValuesItem.leftItem()),
                            reachableValuesItem.leftSorter(),
                            sortedValues, sorter);
            return sortedValues;
        } else {
            throw new IllegalStateException(
                    "Impossible state: the reachable values structure for variable (%s) with sorter (%s) does not align with the existing ascending (%s) and descending (%s) sorters."
                            .formatted(variableDescriptor, sorter, reachableValuesItem.leftSorter(),
                                    reachableValuesItem.rightSorter()));
        }
    }

    private ReachableValues<Entity_, Value_> buildSortedReachableValues(ReachableValues<Entity_, Value_> values,
            SelectionSorter<Solution_, Value_> sorter, boolean clear) {
        var sortedValues = values.copy(SelectionSorterAdapter.of(cachedWorkingSolution, sorter));
        if (clear) {
            values.clear();
        }
        return sortedValues;
    }

    private ReachableValues<Entity_, Value_> fetchReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var entityDescriptor = variableDescriptor.getEntityDescriptor();
        var entityList = (List<Entity_>) entityDescriptor.extractEntities(cachedWorkingSolution);
        var entityIndexMap = buildIndexMap(entityList.iterator(), entityList.size());
        var entityIndexItem = new ReachableValuesIndex<>(entityIndexMap, entityList);
        var valueList = getFromSolution(cachedWorkingSolution, null);
        var valueIndexMap = buildIndexMap(valueList.createOriginalIterator(), (int) valueList.getSize());
        var valueListSize = valueList.getSize();
        if (valueListSize > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "The structure %s cannot be built for the entity %s (%s) because value range has a size (%d) which is higher than Integer.MAX_VALUE."
                            .formatted(ReachableValues.class.getSimpleName(),
                                    entityDescriptor.getEntityClass().getSimpleName(),
                                    variableDescriptor.getVariableName(), valueListSize));
        }
        var expectedTypeOfValue = valueRangeDescriptor.getVariableDescriptor()
                .getVariableMetaModel()
                .type();
        var reachableValueList = initReachableValueList(valueList, entityList.size());
        var valueIndexItem = new ReachableValuesIndex<>(valueIndexMap, reachableValueList);
        for (var i = 0; i < entityList.size(); i++) {
            var entity = entityList.get(i);
            var valueRange = getFromEntity(entity, entityList.size(), null);
            loadEntityValueRange(i, valueIndexMap, valueRange, reachableValueList);
        }
        var sorterAdapter = sorter != null ? SelectionSorterAdapter.of(cachedWorkingSolution, sorter) : null;
        return new ReachableValues<>(entityIndexItem, valueIndexItem, expectedTypeOfValue, sorterAdapter,
                variableDescriptor.getValueRangeDescriptor().acceptsNullInValueRange());
    }

    private static <Type_> Map<Type_, Integer> buildIndexMap(Iterator<@Nullable Type_> allValues, int size) {
        Map<Type_, Integer> indexMap = CollectionUtils.newHashMap(size);
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

    private List<ReachableItemValue<Entity_, Value_>> initReachableValueList(CountableValueRange<Value_> valueRange,
            int entityListSize) {
        var valuesSize = (int) valueRange.getSize();
        Iterator<@Nullable Value_> iterator = valueRange.createOriginalIterator();
        var spliterator = Spliterators.spliterator(iterator, valuesSize, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        var idx = new MutableInt(-1);
        return StreamSupport.stream(spliterator, false)
                .filter(Objects::nonNull)
                .map(v -> new ReachableItemValue<Entity_, Value_>(idx.increment(), v, entityListSize, valuesSize))
                .toList();
    }

    private static <Entity_, Value_> void loadEntityValueRange(int entityIndex, Map<Value_, Integer> valueIndexMap,
            CountableValueRange<Value_> valueRange, List<ReachableItemValue<Entity_, Value_>> reachableValueList) {
        // We create a bitset containing all possible values from the range to optimize operations
        var allValuesBitSet = buildBitSetForValueRange(valueRange, valueIndexMap);
        // The second pass need only to iterate over the bits we already set.
        var valueIndex = allValuesBitSet.nextSetBit(0);
        while (valueIndex >= 0) {
            var item = reachableValueList.get(valueIndex);
            item.addEntity(entityIndex);
            // We unset the current value index to import only the values that are reachable
            item.addValuesExcept(allValuesBitSet, valueIndex);
            valueIndex = allValuesBitSet.nextSetBit(valueIndex + 1);
        }
    }

    private static <Value_> BitSet buildBitSetForValueRange(CountableValueRange<Value_> valueRange,
            Map<Value_, Integer> valueIndexMap) {
        var valueBitSet = new BitSet((int) valueRange.getSize());
        Iterator<@Nullable Value_> iterator = valueRange.createOriginalIterator();
        while (iterator.hasNext()) {
            var value = iterator.next();
            if (value == null) {
                continue;
            }
            valueBitSet.set(valueIndexMap.get(value));
        }
        return valueBitSet;
    }

    private record ValueRangeItem<Solution_, Entity_, Type_, Value_>(@Nullable Entity_ entity, @Nullable Type_ leftItem,
            @Nullable SelectionSorter<Solution_, Value_> leftSorter, @Nullable Type_ rightItem,
            @Nullable SelectionSorter<Solution_, Value_> rightSorter) {

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_>
                ofEntity(Entity_ entity) {
            return new ValueRangeItem<>(entity, null, null, null, null);
        }

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_> ofLeft(
                @Nullable Entity_ entity,
                Type_ leftItem, @Nullable SelectionSorter<Solution_, Value_> leftSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, null, null);
        }

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_> of(
                @Nullable Entity_ entity, @Nullable Type_ leftItem, @Nullable SelectionSorter<Solution_, Value_> leftSorter,
                @Nullable Type_ rightItem,
                @Nullable SelectionSorter<Solution_, Value_> rightSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, rightItem, rightSorter);
        }

    }

    /**
     * The record holds a reference to {@link CountableValueRange},
     * a precomputed hash to avoid recalculating it every time.
     */
    private record HashedValueRange<T>(CountableValueRange<T> item, int hash) {

        public static <Value_> HashedValueRange<Value_> of(CountableValueRange<Value_> valueRange) {
            return new HashedValueRange<>(valueRange, valueRange.hashCode());
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ValueRangeState.HashedValueRange<?> that)) {
                return false;
            }
            return hash == that.hash
                    && Objects.equals(item, that.item);
        }
    }

}
