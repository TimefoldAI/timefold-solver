package ai.timefold.solver.core.impl.domain.valuerange.sort;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface SortableValueRange<T> {

    /**
     * The sorting operation copies the current value range and sorts it using the provided sorter.
     *
     * @param sorter never null, the value range sorter
     * @return A new instance of the value range, with the data sorted.
     */
    ValueRange<T> sort(ValueRangeSorter<T> sorter);
}
