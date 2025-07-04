package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;

public final class IterableFilteringValueSelector<Solution_>
        extends FilteringValueSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    IterableFilteringValueSelector(IterableValueSelector<Solution_> childValueSelector,
            SelectionFilter<Solution_, Object> filter) {
        super(childValueSelector, filter);
    }

    @Override
    public long getSize() {
        return ((IterableValueSelector<Solution_>) childValueSelector).getSize();
    }

    @Override
    public Iterator<Object> iterator() {
        return new JustInTimeFilteringValueIterator(((IterableValueSelector<Solution_>) childValueSelector).iterator(),
                determineBailOutSize());
    }

    private long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        return ((IterableValueSelector<Solution_>) childValueSelector).getSize() * 10L;
    }

}
