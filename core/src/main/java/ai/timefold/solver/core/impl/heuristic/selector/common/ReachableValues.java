package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class records the relationship between each planning value and all entities that include the related value
 * within its value range.
 *
 * @see FromEntityPropertyValueRangeDescriptor
 */
@NullMarked
public final class ReachableValues<E, V> {

    private final Map<V, ReachableItemValue<E, V>> values;
    private final @Nullable Class<?> valueClass;
    private final @Nullable ValueRangeSorter<V> valueRangeSorter;
    private final boolean acceptsNullValue;
    private @Nullable ReachableItemValue<E, V> firstCachedObject;
    private @Nullable ReachableItemValue<E, V> secondCachedObject;

    public ReachableValues(Map<V, ReachableItemValue<E, V>> values, @Nullable Class<?> valueClass,
            @Nullable ValueRangeSorter<V> valueRangeSorter, boolean acceptsNullValue) {
        this.values = values;
        this.valueClass = valueClass;
        this.valueRangeSorter = valueRangeSorter;
        this.acceptsNullValue = acceptsNullValue;
    }

    private @Nullable ReachableItemValue<E, V> fetchItemValue(V value) {
        ReachableItemValue<E, V> selected = null;
        if (firstCachedObject != null && firstCachedObject.value == value) {
            selected = firstCachedObject;
        } else if (secondCachedObject != null && secondCachedObject.value == value) {
            selected = secondCachedObject;
            // The most recently used item is moved to the first position.
            // The goal is to try to keep recently used items in the cache.
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        if (selected == null) {
            selected = values.get(value);
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        return selected;
    }

    public List<E> extractEntitiesAsList(V value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.randomAccessEntityList;
    }

    public List<V> extractValuesAsList(V value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        itemValue.checkSorting(valueRangeSorter);
        return itemValue.randomAccessValueList;
    }

    public int getSize() {
        return values.size();
    }

    public boolean isEntityReachable(@Nullable V origin, @Nullable E entity) {
        if (entity == null) {
            return true;
        }
        if (origin == null) {
            return acceptsNullValue;
        }
        var originItemValue = fetchItemValue(origin);
        if (originItemValue == null) {
            return false;
        }
        return originItemValue.entityMap.containsKey(entity);
    }

    public boolean isValueReachable(V origin, @Nullable V otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        return originItemValue.valueMap.containsKey(Objects.requireNonNull(otherValue));
    }

    public boolean acceptsNullValue() {
        return acceptsNullValue;
    }

    public boolean matchesValueClass(V value) {
        return valueClass != null && valueClass.isAssignableFrom(Objects.requireNonNull(value).getClass());
    }

    public @Nullable SelectionSorter<?, V> getValueSelectionSorter() {
        return valueRangeSorter != null ? valueRangeSorter.getInnerSorter() : null;
    }

    public ReachableValues<E, V> copy(@Nullable ValueRangeSorter<V> valueRangeSorter) {
        Map<V, ReachableItemValue<E, V>> newValues = ConfigUtils.isGenericTypeImmutable(valueClass)
                ? new HashMap<>(values.size())
                : new IdentityHashMap<>(values.size());
        for (Map.Entry<V, ReachableItemValue<E, V>> entry : values.entrySet()) {
            newValues.put(entry.getKey(), entry.getValue().copy());
        }
        return new ReachableValues<>(newValues, valueClass, valueRangeSorter, acceptsNullValue);
    }

    @NullMarked
    public static final class ReachableItemValue<E, V> {
        private final V value;
        private final Map<E, E> entityMap;
        private final Map<V, V> valueMap;
        private final List<E> randomAccessEntityList;
        private final List<V> randomAccessValueList;
        private boolean sorted = false;

        public ReachableItemValue(V value, int entityListSize, int valueListSize) {
            this.value = value;
            this.entityMap = new IdentityHashMap<>(entityListSize);
            this.randomAccessEntityList = new ArrayList<>(entityListSize);
            this.valueMap = ConfigUtils.isGenericTypeImmutable(value.getClass()) ? new LinkedHashMap<>(valueListSize)
                    : new IdentityHashMap<>(valueListSize);
            this.randomAccessValueList = new ArrayList<>(valueListSize);
        }

        private ReachableItemValue(V value, Map<E, E> entityMap, Map<V, V> valueMap, List<E> randomAccessEntityList,
                List<V> randomAccessValueList) {
            this.value = value;
            this.entityMap = entityMap;
            this.valueMap = valueMap;
            this.randomAccessEntityList = randomAccessEntityList;
            this.randomAccessValueList = randomAccessValueList;
        }

        public void addEntity(E entity) {
            if (entityMap.put(entity, entity) == null) {
                randomAccessEntityList.add(entity);
            }
        }

        public void addValue(V value) {
            if (valueMap.put(value, value) == null) {
                randomAccessValueList.add(value);
            }
        }

        private void checkSorting(@Nullable ValueRangeSorter<V> valueRangeSorter) {
            if (valueRangeSorter != null && !sorted) {
                var sortedList = valueRangeSorter.sort(randomAccessValueList);
                randomAccessValueList.clear();
                randomAccessValueList.addAll(sortedList);
                sorted = true;
            }
        }

        public ReachableItemValue<E, V> copy() {
            return new ReachableItemValue<>(value, entityMap, valueMap, new ArrayList<>(randomAccessEntityList),
                    new ArrayList<>(randomAccessValueList));
        }
    }

}
