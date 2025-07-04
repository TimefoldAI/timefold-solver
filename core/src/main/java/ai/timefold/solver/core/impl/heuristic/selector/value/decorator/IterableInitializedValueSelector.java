package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;

public final class IterableInitializedValueSelector<Solution_>
        extends InitializedValueSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    public IterableInitializedValueSelector(IterableValueSelector<Solution_> childValueSelector) {
        super(childValueSelector);
    }

    @Override
    public long getSize() {
        return ((IterableValueSelector<Solution_>) childValueSelector).getSize();
    }

    @Override
    public Iterator<Object> iterator() {
        return new JustInTimeInitializedValueIterator(
                ((IterableValueSelector<Solution_>) childValueSelector).iterator(), determineBailOutSize());
    }

    protected long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        return ((IterableValueSelector<Solution_>) childValueSelector).getSize() * 10L;
    }

}
