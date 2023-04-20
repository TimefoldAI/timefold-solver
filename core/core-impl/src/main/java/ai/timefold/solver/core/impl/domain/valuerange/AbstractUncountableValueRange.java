package ai.timefold.solver.core.impl.domain.valuerange;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;

/**
 * Abstract superclass for {@link ValueRange} that is not a {@link CountableValueRange}).
 *
 * @see ValueRange
 * @see ValueRangeFactory
 */
public abstract class AbstractUncountableValueRange<T> implements ValueRange<T> {

}
