package ai.timefold.solver.core.impl.domain.valuerange;

import java.time.OffsetDateTime;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;

/**
 * Abstract superclass for {@link CountableValueRange} (and therefore {@link ValueRange}).
 *
 * @see CountableValueRange
 * @see ValueRange
 * @see ValueRangeFactory
 */
public abstract class AbstractCountableValueRange<T> implements CountableValueRange<T> {

    /**
     * Certain optimizations can be applied if {@link Object#equals(Object)} can be relied upon
     * to determine that two objects are the same.
     * Typically, this is not guaranteed for user-provided objects,
     * but is true for many builtin types and classes,
     * such as {@link String}, {@link Integer}, {@link OffsetDateTime}, etc.
     * 
     * @return true if we should trust {@link Object#equals(Object)} in this value range
     */
    public boolean isValueImmutable() {
        return true; // Override in subclasses if needed.
    }

    @Override
    public boolean isEmpty() {
        return getSize() == 0L;
    }

}
