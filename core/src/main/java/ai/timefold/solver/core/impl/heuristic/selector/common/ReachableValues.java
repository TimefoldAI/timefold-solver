package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

    private boolean enableSort = false;
    private final Map<Object, Set<Object>> valueToEntityMap;
    private final Map<Object, Set<Object>> valueToValueMap;
    private final Map<Object, List<Object>> randomAccessValueToEntityMap;
    private final Map<Object, List<Object>> randomAccessValueToValueMap;
    private final @Nullable Class<?> valueClass;

    public ReachableValues(Map<Object, Set<Object>> valueToEntityMap, Map<Object, Set<Object>> valueToValueMap) {
        this.valueToEntityMap = valueToEntityMap;
        this.randomAccessValueToEntityMap = new IdentityHashMap<>(this.valueToEntityMap.size());
        this.valueToValueMap = valueToValueMap;
        this.randomAccessValueToValueMap = new IdentityHashMap<>(this.valueToValueMap.size());
        var first = valueToEntityMap.entrySet().stream().findFirst();
        this.valueClass = first.<Class<?>> map(entry -> entry.getKey().getClass()).orElse(null);
    }

    /**
     * @return all reachable values for the given value.
     */
    public @Nullable Set<Object> extractEntities(Object value) {
        var result = valueToEntityMap.get(value);
        if (enableSort && !(result instanceof SortedSet<Object>)) {
            result = new TreeSet<>(result);
            valueToEntityMap.put(value, result);
        }
        return result;
    }

    /**
     * @return all reachable entities for the given value.
     */
    public @Nullable Set<Object> extractValues(Object value) {
        var result = valueToValueMap.get(value);
        if (enableSort && !(result instanceof SortedSet<Object>)) {
            result = new TreeSet<>(result);
            valueToValueMap.put(value, result);
        }
        return result;
    }

    public List<Object> extractEntitiesAsList(Object value) {
        var result = randomAccessValueToEntityMap.get(value);
        if (result == null) {
            var entitySet = this.valueToEntityMap.get(value);
            if (entitySet != null) {
                result = new ArrayList<>(entitySet);
                if (enableSort) {
                    result = result.stream().sorted().toList();
                }
            } else {
                result = Collections.emptyList();
            }
            randomAccessValueToEntityMap.put(value, result);
        }
        return result;
    }

    public List<Object> extractValuesAsList(Object value) {
        var result = randomAccessValueToValueMap.get(value);
        if (result == null) {
            var valueSet = this.valueToValueMap.get(value);
            if (valueSet != null) {
                result = new ArrayList<>(valueSet);
                if (enableSort) {
                    result = result.stream().sorted().toList();
                }
            } else {
                result = Collections.emptyList();
            }
            randomAccessValueToValueMap.put(value, result);
        }
        return result;
    }

    public int getSize() {
        return valueToEntityMap.size();
    }

    public boolean isValidValueClass(Object value) {
        if (valueToEntityMap.isEmpty()) {
            return false;
        }
        return Objects.requireNonNull(value).getClass().equals(valueClass);
    }

    public void enableSort() {
        this.enableSort = true;
    }

}
