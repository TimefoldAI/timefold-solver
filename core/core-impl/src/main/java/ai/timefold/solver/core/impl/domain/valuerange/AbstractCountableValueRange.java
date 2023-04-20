package ai.timefold.solver.core.impl.domain.valuerange;

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

    @Override
    public boolean isEmpty() {
        return getSize() == 0L;
    }

}
