package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class records the relationship between each planning value and all entities that include the related value
 * within its value range.
 *
 * @see FromEntityPropertyValueRangeDescriptor
 */
@NullMarked
public final class ReachableValues {

    private final Map<Object, ReachableItemValue> values;
    private final @Nullable Class<?> valueClass;
    private final boolean acceptsNullValue;
    private @Nullable ReachableItemValue cachedObject;

    public ReachableValues(Map<Object, ReachableItemValue> values, boolean acceptsNullValue) {
        this.values = values;
        this.acceptsNullValue = acceptsNullValue;
        var firstValue = values.entrySet().stream().findFirst();
        this.valueClass = firstValue.<Class<?>> map(entry -> entry.getKey().getClass()).orElse(null);
    }

    private @Nullable ReachableItemValue fetchItemValue(Object value) {
        if (cachedObject == null || value != cachedObject.value) {
            cachedObject = values.get(value);
        }
        return cachedObject;
    }

    public List<Object> extractEntitiesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.randomAccessEntityList;
    }

    public List<Object> extractValuesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.randomAccessValueList;
    }

    public int getSize() {
        return values.size();
    }

    public boolean isEntityReachable(Object origin, @Nullable Object entity) {
        if (entity == null) {
            return true;
        }
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        return originItemValue.entitySet.contains(entity);
    }

    public boolean isValueReachable(Object origin, @Nullable Object otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        return originItemValue.valueSet.contains(Objects.requireNonNull(otherValue));
    }

    public boolean matchesValueClass(Object value) {
        return valueClass != null && valueClass.isAssignableFrom(Objects.requireNonNull(value).getClass());
    }

    @NullMarked
    public static final class ReachableItemValue {
        private final Object value;
        private final Set<Object> entitySet;
        private final Set<Object> valueSet;
        private final List<Object> randomAccessEntityList;
        private final List<Object> randomAccessValueList;

        public ReachableItemValue(Object value, int entityListSize, int valueListSize) {
            this.value = value;
            this.entitySet = new LinkedHashSet<>(entityListSize);
            this.randomAccessEntityList = new ArrayList<>(entityListSize);
            this.valueSet = new LinkedHashSet<>(valueListSize);
            this.randomAccessValueList = new ArrayList<>(valueListSize);
        }

        public void addEntity(Object entity) {
            if (entitySet.add(entity)) {
                randomAccessEntityList.add(entity);
            }
        }

        public void addValue(Object value) {
            if (valueSet.add(value)) {
                randomAccessValueList.add(value);
            }
        }
    }

}
