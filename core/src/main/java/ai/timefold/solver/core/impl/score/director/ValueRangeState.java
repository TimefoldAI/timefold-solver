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
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableValuesIndex;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ValueRangeState<Solution_, T> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final Solution_ cachedWorkingSolution;

    // Solution related fields
    private @Nullable ValueRangeItem<Solution_, CountableValueRange<T>> fromSolutionItem;
    private @Nullable Map<Object, Integer> fromSolutionValueIndexMap;

    // Entity related fields
    private @Nullable Map<Object, ValueRangeItem<Solution_, CountableValueRange<T>>> fromEntityMap;
    private @Nullable Map<BitSetItem, Object> fromEntityBitSet;

    // ReachableValues related field
    private @Nullable ValueRangeItem<Solution_, ReachableValues> reachableValuesItem;

    ValueRangeState(ValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ cachedWorkingSolution) {
        this.valueRangeDescriptor = Objects.requireNonNull(valueRangeDescriptor);
        this.cachedWorkingSolution = Objects.requireNonNull(cachedWorkingSolution);
    }

    public CountableValueRange<T> getFromSolution(Solution_ solution, @Nullable SelectionSorter<Solution_, ?> sorter) {
        // No item, we set the left side by default
        if (fromSolutionItem == null) {
            var valueRange = fetchValueRangeFromSolution(solution, sorter);
            fromSolutionItem = ValueRangeItem.ofLeft(null, valueRange, sorter);
            fromSolutionValueIndexMap = buildIndexMap((Iterator<Object>) valueRange.createOriginalIterator(),
                    (int) valueRange.getSize(), valueRangeDescriptor.isGenericTypeImmutable());
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
            fromSolutionValueIndexMap = buildIndexMap((Iterator<Object>) sortedValueRange.createOriginalIterator(),
                    (int) sortedValueRange.getSize(), valueRangeDescriptor.isGenericTypeImmutable());
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

    private @Nullable <V> V pickValueBySorter(ValueRangeItem<Solution_, V> item,
            @Nullable SelectionSorter<Solution_, ?> sorter,
            @Nullable BiFunction<Object, @Nullable SelectionSorter<Solution_, ?>, V> placeholderFunction) {
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

    private Map<Object, Integer> getIndexMapFromSolution() {
        if (fromSolutionValueIndexMap == null) {
            // We call getFromSolution to ensure the solution-range is loaded and the related index map is created
            getFromSolution(cachedWorkingSolution, null);
        }
        return fromSolutionValueIndexMap;
    }

    private CountableValueRange<T> fetchValueRangeFromSolution(Solution_ solution,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        CountableValueRange<T> valueRange = extractValueRange(valueRangeDescriptor, solution);
        return sortValueRange(valueRange, sorter);
    }

    private CountableValueRange<T> extractValueRange(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
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

    @SuppressWarnings("unchecked")
    private CountableValueRange<T> sortValueRange(CountableValueRange<T> originalValueRange,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        if (sorter == null) {
            return originalValueRange;
        }
        if (!(originalValueRange instanceof SortableValueRange sortableValueRange)) {
            throw new IllegalStateException("Impossible state: value range (%s) on planning solution (%s) is not sortable."
                    .formatted(valueRangeDescriptor, cachedWorkingSolution));
        }
        var sorterAdapter = SelectionSorterAdapter.of(cachedWorkingSolution, sorter);
        return (CountableValueRange<T>) sortableValueRange.sort(sorterAdapter);
    }

    public CountableValueRange<T> getFromEntity(Object entity, int entityCount,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
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

    private Map<Object, ValueRangeItem<Solution_, CountableValueRange<T>>> ensureEntityMapIsInitialized(int entityCount) {
        if (fromEntityMap == null) {
            fromEntityMap = CollectionUtils.newIdentityHashMap(entityCount);
            fromEntityBitSet = CollectionUtils.newHashMap(entityCount);
        }
        return fromEntityMap;
    }

    private ValueRangeItem<Solution_, CountableValueRange<T>> buildEntityValueRangeItem(Object entity,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        var valueRange = fetchValueRangeFromEntity(entity, sorter);
        var entityMatch = findEntityBitSetMatch(entity, valueRange);
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
    private @Nullable Object findEntityBitSetMatch(Object entity, CountableValueRange<T> valueRange) {
        // We create a BitSet from the range values
        // and check if another identical range already exists to prevent duplication
        var valueIndexMap = getIndexMapFromSolution();
        var valueRangeBitSet = buildBitSetForValueRange(valueRange, valueIndexMap);
        var bitSetItem = BitSetItem.of(valueRange, valueRangeBitSet);
        var fromEntity = fromEntityBitSet.get(bitSetItem);
        if (fromEntity == null) {
            fromEntityBitSet.put(bitSetItem, entity);
            return null;
        }
        return fromEntity;
    }

    private CountableValueRange<T> fetchValueRangeFromEntity(Object entity, @Nullable SelectionSorter<Solution_, ?> sorter) {
        CountableValueRange<T> valueRange;
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
        } else {
            valueRange = countableValueRange;
        }
        return sortValueRange(valueRange, sorter);
    }

    private BitSet buildBitSetForValueRange(CountableValueRange<T> valueRange, Map<Object, Integer> valueIndexMap) {
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

    public ReachableValues getReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
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

    private ReachableValues buildSortedReachableValues(ReachableValues values, SelectionSorter<Solution_, ?> sorter,
            boolean clear) {
        var sorterAdapter = SelectionSorterAdapter.of(cachedWorkingSolution, sorter);
        var sortedValues = values.copy(sorterAdapter);
        if (clear) {
            values.clear();
        }
        return sortedValues;
    }

    private ReachableValues fetchReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable SelectionSorter<Solution_, ?> sorter) {
        var entityDescriptor = variableDescriptor.getEntityDescriptor();
        var entityList = entityDescriptor.extractEntities(cachedWorkingSolution);
        var entityIndexMap = buildIndexMap(entityList.iterator(), entityList.size(), false);
        var entityIndexItem = new ReachableValuesIndex<>(entityIndexMap, entityList);
        var valueList = getFromSolution(cachedWorkingSolution, null);
        var valueIndexMap = buildIndexMap((Iterator<Object>) valueList.createOriginalIterator(), (int) valueList.getSize(),
                valueRangeDescriptor.isGenericTypeImmutable());
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
        return new ReachableValues(entityIndexItem, valueIndexItem, expectedTypeOfValue, sorterAdapter,
                variableDescriptor.getValueRangeDescriptor().acceptsNullInValueRange());
    }

    private static Map<Object, Integer> buildIndexMap(Iterator<@Nullable Object> allValues, int size, boolean isImmutable) {
        Map<Object, Integer> indexMap =
                isImmutable ? CollectionUtils.newHashMap(size) : CollectionUtils.newIdentityHashMap(size);
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

    private List<ReachableValues.ReachableItemValue> initReachableValueList(CountableValueRange<T> valueRange,
            int entityListSize) {
        var valuesSize = (int) valueRange.getSize();
        Iterator<@Nullable T> iterator = valueRange.createOriginalIterator();
        var spliterator = Spliterators.spliterator(iterator, valuesSize, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        var idx = new MutableInt(-1);
        return StreamSupport.stream(spliterator, false)
                .filter(Objects::nonNull)
                .map(v -> new ReachableValues.ReachableItemValue(idx.increment(), v, entityListSize, valuesSize))
                .toList();
    }

    private void loadEntityValueRange(int entityIndex, Map<Object, Integer> valueIndexMap,
            CountableValueRange<T> valueRange, List<ReachableValues.ReachableItemValue> reachableValueList) {
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

    private record ValueRangeItem<Solution_, T>(@Nullable Object entity, @Nullable T leftItem,
            @Nullable SelectionSorter<Solution_, ?> leftSorter, @Nullable T rightItem,
            @Nullable SelectionSorter<Solution_, ?> rightSorter) {

        public static <Solution_, T> ValueRangeItem<Solution_, T> ofEntity(Object entity) {
            return new ValueRangeItem<>(entity, null, null, null, null);
        }

        public static <Solution_, T> ValueRangeItem<Solution_, T> ofLeft(@Nullable Object entity, T leftItem,
                @Nullable SelectionSorter<Solution_, ?> leftSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, null, null);
        }

        public static <Solution_, T> ValueRangeItem<Solution_, T> of(@Nullable Object entity, @Nullable T leftItem,
                @Nullable SelectionSorter<Solution_, ?> leftSorter, @Nullable T rightItem,
                @Nullable SelectionSorter<Solution_, ?> rightSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, rightItem, rightSorter);
        }

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
