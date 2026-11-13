package ai.timefold.solver.core.impl.score.director;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.NullAllowingValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SelectionSorterAdapter;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.RangeIndex;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableItemValue;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues.ReachableValuesIndex;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.util.MutableInt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ValueRangeState<Solution_, Entity_, Value_> {

    private final AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final Solution_ cachedWorkingSolution;

    // Solution related fields
    private @Nullable ValueRangeItem<Solution_, Entity_, ValueRange<Value_>, Value_> fromSolutionItem;

    // Entity related fields
    private @Nullable Map<Entity_, ValueRangeItem<Solution_, Entity_, ValueRange<Value_>, Value_>> fromEntityMap;
    private @Nullable Map<HashedValueRange<Value_>, Entity_> valueRangeDeduplicationCache;

    // ReachableValues related field
    private @Nullable ValueRangeItem<Solution_, Entity_, ReachableValues<Entity_, Value_>, Value_> reachableValuesItem;
    private @Nullable List<Value_> movableValueList;

    ValueRangeState(AbstractValueRangeDescriptor<Solution_> valueRangeDescriptor, Solution_ cachedWorkingSolution) {
        this.valueRangeDescriptor = Objects.requireNonNull(valueRangeDescriptor);
        this.cachedWorkingSolution = Objects.requireNonNull(cachedWorkingSolution);
    }

    public ValueRange<Value_> getFromSolution(Solution_ solution, @Nullable SelectionSorter<Solution_, Value_> sorter) {
        // No item, we set the left side by default
        if (fromSolutionItem == null) {
            var valueRange = fetchValueRangeFromSolution(solution, sorter);
            fromSolutionItem = ValueRangeItem.ofLeft(null, valueRange, sorter);
            return valueRange;
        }
        var valueRange = pickValueBySorter(fromSolutionItem, sorter, null);
        if (valueRange != null) {
            return valueRange;
        }
        // If the left sorter is null and the given sorter is not, we replace the left value with a sorted one.
        if (fromSolutionItem.leftSorter() == null && sorter != null) {
            var sortedValueRange = sortValueRange(Objects.requireNonNull(fromSolutionItem.leftItem()), sorter);
            fromSolutionItem = ValueRangeItem.of(null, sortedValueRange, sorter, fromSolutionItem.rightItem(),
                    fromSolutionItem.rightSorter());
            return sortedValueRange;
        } else if (fromSolutionItem.rightItem() == null) {
            var sortedValueRange = sortValueRange(Objects.requireNonNull(fromSolutionItem.leftItem()), sorter);
            fromSolutionItem = ValueRangeItem.of(null, fromSolutionItem.leftItem(), fromSolutionItem.leftSorter(),
                    sortedValueRange, sorter);
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

    private ValueRange<Value_> fetchValueRangeFromSolution(Solution_ solution,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var valueRange = extractValueRange(solution);
        return sortValueRange(valueRange, sorter);
    }

    private ValueRange<Value_> extractValueRange(Solution_ solution) {
        var extractedValueRange = valueRangeDescriptor.<Value_> extractAllValues(Objects.requireNonNull(solution));
        if (valueRangeDescriptor.acceptsNullInValueRange()) {
            return new NullAllowingValueRange<>(extractedValueRange);
        } else {
            return extractedValueRange;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ValueRange<Value_> sortValueRange(ValueRange<Value_> originalValueRange,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        if (sorter == null) {
            return originalValueRange;
        }
        if (!(originalValueRange instanceof SortableValueRange sortableValueRange)) {
            throw new IllegalStateException("Impossible state: value range (%s) on planning solution (%s) is not sortable."
                    .formatted(valueRangeDescriptor, cachedWorkingSolution));
        }
        return sortableValueRange.sort(SelectionSorterAdapter.of(cachedWorkingSolution, sorter));
    }

    public ValueRange<Value_> getFromEntity(Entity_ entity,
            @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var entityMap = ensureEntityMapIsInitialized();
        var item = entityMap.get(entity);
        // No item, we set the left side by default
        if (item == null) {
            var newItem = buildEntityValueRangeItem(entity, sorter);
            entityMap.put(entity, newItem);
            if (newItem.entity() != null && newItem.leftItem() == null && newItem.rightItem() == null) {
                // Placeholder for another entity
                return getFromEntity(Objects.requireNonNull(newItem.entity()), sorter);
            }
            return Objects.requireNonNull(newItem.leftItem());
        }
        var valueRange = pickValueBySorter(item, sorter, this::getFromEntity);
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

    private Map<Entity_, ValueRangeItem<Solution_, Entity_, ValueRange<Value_>, Value_>>
            ensureEntityMapIsInitialized() {
        if (fromEntityMap == null) {
            fromEntityMap = new IdentityHashMap<>();
            valueRangeDeduplicationCache = new HashMap<>();
        }
        return fromEntityMap;
    }

    private ValueRangeItem<Solution_, Entity_, ValueRange<Value_>, Value_> buildEntityValueRangeItem(Entity_ entity,
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
    private @Nullable Entity_ findEntityMatch(Entity_ entity, ValueRange<Value_> valueRange) {
        var hashedValueRange = HashedValueRange.of(valueRange);
        var fromEntity = valueRangeDeduplicationCache.get(hashedValueRange);
        if (fromEntity == null) {
            valueRangeDeduplicationCache.put(hashedValueRange, entity);
            return null;
        }
        return fromEntity;
    }

    private ValueRange<Value_> fetchValueRangeFromEntity(Entity_ entity, @Nullable SelectionSorter<Solution_, Value_> sorter) {
        var extractedValueRange =
                valueRangeDescriptor.<Value_> extractValuesFromEntity(cachedWorkingSolution, Objects.requireNonNull(entity));
        var valueRange = valueRangeDescriptor.acceptsNullInValueRange() ? new NullAllowingValueRange<>(extractedValueRange)
                : extractedValueRange;
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
            reachableValuesItem = ValueRangeItem.of(null, sortedValues, sorter, reachableValuesItem.rightItem(),
                    reachableValuesItem.rightSorter());
            return sortedValues;
        } else if (reachableValuesItem.rightItem() == null && sorter != null) {
            // We use the existing left item to create a new structure with a different sorter
            var sortedValues =
                    buildSortedReachableValues(Objects.requireNonNull(reachableValuesItem.leftItem()), sorter, false);
            reachableValuesItem = ValueRangeItem.of(null, Objects.requireNonNull(reachableValuesItem.leftItem()),
                    reachableValuesItem.leftSorter(), sortedValues, sorter);
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
                            .formatted(ReachableValues.class.getSimpleName(), entityDescriptor.getEntityClass().getSimpleName(),
                                    variableDescriptor.getVariableName(), valueListSize));
        }
        var expectedTypeOfValue = valueRangeDescriptor.getVariableDescriptor().getVariableMetaModel().type();
        // Equal ranges are deduplicated by getFromEntity, so entities with equal ranges share one range index.
        var entityIndicesByRange = new IdentityHashMap<ValueRange<Value_>, List<Integer>>();
        for (var i = 0; i < entityList.size(); i++) {
            var entity = entityList.get(i);
            var valueRange = getFromEntity(entity, null);
            entityIndicesByRange.computeIfAbsent(valueRange, k -> new ArrayList<>()).add(i);
        }
        var rangeCount = entityIndicesByRange.size();
        var reachableValueList = initReachableValueList(valueList, rangeCount);
        var valueIndexItem = new ReachableValuesIndex<>(valueIndexMap, reachableValueList);
        var rangeOfEntity = new int[entityList.size()];
        var entitiesOfRange = new int[rangeCount][];
        var valuesOfRange = new BitSet[rangeCount];
        var rangeIndex = 0;
        for (var entry : entityIndicesByRange.entrySet()) {
            var entityIndices = entry.getValue();
            var rangeEntityIndices = new int[entityIndices.size()];
            for (var i = 0; i < rangeEntityIndices.length; i++) {
                rangeEntityIndices[i] = entityIndices.get(i);
                rangeOfEntity[rangeEntityIndices[i]] = rangeIndex;
            }
            entitiesOfRange[rangeIndex] = rangeEntityIndices;
            valuesOfRange[rangeIndex] = loadEntityValueRange(rangeIndex, valueIndexMap, entry.getKey(), reachableValueList);
            rangeIndex++;
        }
        var groupByRanges = new HashMap<BitSet, Integer>();
        for (var item : reachableValueList) {
            item.setGroup(groupByRanges.computeIfAbsent(item.getRanges(), k -> groupByRanges.size()));
        }
        var rangeIndexItem = new RangeIndex(rangeOfEntity, entitiesOfRange, valuesOfRange, groupByRanges.size());
        var sorterAdapter = sorter != null ? SelectionSorterAdapter.of(cachedWorkingSolution, sorter) : null;
        Predicate<Entity_> movableEntityFilter = entityDescriptor.createMovableEntityFilter(cachedWorkingSolution);
        Predicate<Value_> movableValueFilter = movableValueFilter(variableDescriptor, cachedWorkingSolution);
        return new ReachableValues<>(entityIndexItem, valueIndexItem, rangeIndexItem, expectedTypeOfValue, sorterAdapter,
                variableDescriptor.getValueRangeDescriptor().acceptsNullInValueRange(), movableEntityFilter,
                movableValueFilter);
    }

    private static <Type_> Map<Type_, Integer> buildIndexMap(Iterator<@Nullable Type_> allValues, int size) {
        Map<Type_, Integer> indexMap = HashMap.newHashMap(size);
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

    private List<ReachableItemValue<Value_>> initReachableValueList(ValueRange<Value_> valueRange,
            int rangeCount) {
        var valuesSize = (int) valueRange.getSize();
        var iterator = valueRange.createOriginalIterator();
        var spliterator = Spliterators.spliterator(iterator, valuesSize, Spliterator.ORDERED | Spliterator.IMMUTABLE);
        var idx = new MutableInt(-1);
        return StreamSupport.stream(spliterator, false)
                .filter(Objects::nonNull)
                .map(v -> new ReachableItemValue<>(idx.increment(), v, rangeCount)).toList();
    }

    /**
     * @return the indices of the values in the range
     */
    private static <Value_> BitSet loadEntityValueRange(int rangeIndex, Map<Value_, Integer> valueIndexMap,
            ValueRange<Value_> valueRange, List<ReachableItemValue<Value_>> reachableValueList) {
        var valueIndices = buildBitSetForValueRange(valueRange, valueIndexMap);
        for (var valueIndex = valueIndices.nextSetBit(0); valueIndex >= 0; valueIndex =
                valueIndices.nextSetBit(valueIndex + 1)) {
            reachableValueList.get(valueIndex).addRange(rangeIndex);
        }
        return valueIndices;
    }

    private static <Value_> BitSet buildBitSetForValueRange(ValueRange<Value_> valueRange, Map<Value_, Integer> valueIndexMap) {
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

    private record ValueRangeItem<Solution_, Entity_, Type_, Value_>(@Nullable Entity_ entity, @Nullable Type_ leftItem,
            @Nullable SelectionSorter<Solution_, Value_> leftSorter,
            @Nullable Type_ rightItem,
            @Nullable SelectionSorter<Solution_, Value_> rightSorter) {

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_>
                ofEntity(Entity_ entity) {
            return new ValueRangeItem<>(entity, null, null, null, null);
        }

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_>
                ofLeft(@Nullable Entity_ entity, Type_ leftItem, @Nullable SelectionSorter<Solution_, Value_> leftSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, null, null);
        }

        public static <Solution_, Entity_, Type_, Value_> ValueRangeItem<Solution_, Entity_, Type_, Value_> of(
                @Nullable Entity_ entity, @Nullable Type_ leftItem, @Nullable SelectionSorter<Solution_, Value_> leftSorter,
                @Nullable Type_ rightItem, @Nullable SelectionSorter<Solution_, Value_> rightSorter) {
            return new ValueRangeItem<>(entity, leftItem, leftSorter, rightItem, rightSorter);
        }

    }

    /**
     * The record holds a reference to {@link ValueRange},
     * a precomputed hash to avoid recalculating it every time.
     */
    private record HashedValueRange<T>(ValueRange<T> item, int hash) {

        public static <Value_> HashedValueRange<Value_> of(ValueRange<Value_> valueRange) {
            return new HashedValueRange<>(valueRange, valueRange.hashCode());
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HashedValueRange<?>(var otherItem, var otherHash)
                    && hash == otherHash
                    && Objects.equals(item, otherItem);
        }
    }

    /**
     * @return unmodifiable, random access; the values of the solution value range, without null and without pinned values
     */
    List<Value_> getMovableValueList(ListVariableDescriptor<Solution_> variableDescriptor) {
        if (movableValueList == null) {
            Predicate<Value_> movableValueFilter = movableValueFilter(variableDescriptor, cachedWorkingSolution);
            var valueList = new ArrayList<Value_>();
            var iterator = getFromSolution(cachedWorkingSolution, null).createOriginalIterator();
            while (iterator.hasNext()) {
                var value = iterator.next();
                if (value != null && (movableValueFilter == null || movableValueFilter.test(value))) {
                    valueList.add(value);
                }
            }
            movableValueList = Collections.unmodifiableList(valueList);
        }
        return movableValueList;
    }

    /**
     * A list variable value is pinned if its entity is pinned, or if it is below the entity's first unpinned index.
     * Each entity is checked with the descriptor of its own class, as in {@link EntityDescriptor#createMovableEntityFilter}.
     * Moves never change which values are pinned; only a problem change does.
     *
     * @return null if no value of the variable can be pinned
     */
    private static <Solution_, Value_> @Nullable Predicate<Value_> movableValueFilter(
            GenuineVariableDescriptor<Solution_> variableDescriptor, Solution_ solution) {
        if (!(variableDescriptor instanceof ListVariableDescriptor<Solution_> listVariableDescriptor)) {
            return null;
        }
        var movableEntityFilter = listVariableDescriptor.getEntityDescriptor().createMovableEntityFilter(solution);
        if (movableEntityFilter == null && !listVariableDescriptor.supportsPinning()) {
            return null;
        }
        return new MovableValueFilter<>(listVariableDescriptor, movableEntityFilter, solution);
    }

    /**
     * Collects the pinned values on the first test, as most users of the filter never test a value.
     */
    @NullMarked
    private static final class MovableValueFilter<Solution_, Value_> implements Predicate<Value_> {

        private final ListVariableDescriptor<Solution_> listVariableDescriptor;
        private final @Nullable Predicate<Object> movableEntityFilter;
        private final Solution_ solution;
        private @Nullable Set<Object> pinnedValueSet;

        private MovableValueFilter(ListVariableDescriptor<Solution_> listVariableDescriptor,
                @Nullable Predicate<Object> movableEntityFilter, Solution_ solution) {
            this.listVariableDescriptor = listVariableDescriptor;
            this.movableEntityFilter = movableEntityFilter;
            this.solution = solution;
        }

        @Override
        public boolean test(Value_ value) {
            if (pinnedValueSet == null) {
                pinnedValueSet = collectPinnedValues();
            }
            return !pinnedValueSet.contains(value);
        }

        private Set<Object> collectPinnedValues() {
            var result = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var entity : listVariableDescriptor.getEntityDescriptor().extractEntities(solution)) {
                var valueList = listVariableDescriptor.getValue(entity);
                // The pinned values of an entity are a prefix of its list; all of them if the entity is pinned.
                var pinnedCount = movableEntityFilter != null && !movableEntityFilter.test(entity) ? valueList.size()
                        : Math.min(valueList.size(), listVariableDescriptor.getFirstUnpinnedIndex(entity));
                result.addAll(valueList.subList(0, pinnedCount));
            }
            return result;
        }

    }

}
