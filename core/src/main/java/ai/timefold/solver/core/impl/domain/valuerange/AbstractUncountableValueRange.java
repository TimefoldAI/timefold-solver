package ai.timefold.solver.core.impl.domain.valuerange;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.impl.domain.valuerange.sort.SortableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

/**
 * Abstract superclass for {@link ValueRange} that is not a {@link CountableValueRange}).
 *
 * @see ValueRange
 * @see ValueRangeFactory
 * @deprecated Uncountable value ranges were never fully supported in many places throughout the solver
 *             and therefore never gained traction.
 *             Use {@link CountableValueRange} instead, and configure a step.
 */
@Deprecated(forRemoval = true, since = "1.1.0")
public abstract class AbstractUncountableValueRange<T> implements ValueRange<T>, SortableValueRange<T> {

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        throw new UnsupportedOperationException();
    }
}
