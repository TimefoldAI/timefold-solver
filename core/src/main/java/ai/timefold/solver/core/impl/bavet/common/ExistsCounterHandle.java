package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

/**
 * Used for filtering in {@link AbstractIfExistsNode}.
 * There is no place where both left and right sets for each counter would be kept together,
 * therefore we create this handle to avoid expensive iteration.
 * (The alternative would be to look up things on the left when we have the right, and vice versa.)
 *
 * @param <LeftTuple_>
 */
final class ExistsCounterHandle<LeftTuple_ extends AbstractTuple> {

    final ExistsCounter<LeftTuple_> counter;
    private final IndexedSet<ExistsCounterHandle<LeftTuple_>> leftSet;
    private final IndexedSet<ExistsCounterHandle<LeftTuple_>> rightSet;
    int leftPosition = -1;
    int rightPosition = -1;

    ExistsCounterHandle(ExistsCounter<LeftTuple_> counter, IndexedSet<ExistsCounterHandle<LeftTuple_>> leftSet,
            IndexedSet<ExistsCounterHandle<LeftTuple_>> rightSet) {
        this.counter = counter;
        this.leftSet = leftSet;
        leftSet.add(this);
        this.rightSet = rightSet;
        rightSet.add(this);
    }

    public void remove() {
        leftSet.remove(this);
        rightSet.remove(this);
    }

}
