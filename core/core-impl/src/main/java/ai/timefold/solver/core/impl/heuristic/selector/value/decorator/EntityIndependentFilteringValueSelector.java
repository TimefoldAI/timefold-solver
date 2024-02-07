package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

public final class EntityIndependentFilteringValueSelector<Solution_>
        extends FilteringValueSelector<Solution_>
        implements EntityIndependentValueSelector<Solution_> {

    EntityIndependentFilteringValueSelector(EntityIndependentValueSelector<Solution_> childValueSelector,
            SelectionFilter<Solution_, Object> filter) {
        super(childValueSelector, filter);
    }

    @Override
    public long getSize() {
        return ((EntityIndependentValueSelector<Solution_>) childValueSelector).getSize();
    }

    @Override
    public Iterator<Object> iterator() {
        return new JustInTimeFilteringValueIterator(((EntityIndependentValueSelector<Solution_>) childValueSelector).iterator(),
                determineBailOutSize());
    }

    private long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        return ((EntityIndependentValueSelector<Solution_>) childValueSelector).getSize() * 10L;
    }

}
