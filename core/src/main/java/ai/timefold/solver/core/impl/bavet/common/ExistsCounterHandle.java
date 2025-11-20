package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

/**
 * Used for filtering in {@link AbstractIfExistsNode}.
 * There is no place where both left and right sets for each counter would be kept together,
 * therefore we create this handle to avoid expensive iteration.
 * (The alternative would be to look up things on the left when we have the right, and vice versa.)
 *
 * @param <LeftTuple_>
 */
@NullMarked
final class ExistsCounterHandle<LeftTuple_ extends AbstractTuple> {

    final ExistsCounter<LeftTuple_> counter;
    final IndexedSet<ExistsCounterHandle<LeftTuple_>> rightHandleSet;
    int leftPosition = -1;
    int rightPosition = -1;

    ExistsCounterHandle(ExistsCounter<LeftTuple_> counter, IndexedSet<ExistsCounterHandle<LeftTuple_>> rightHandleSet) {
        this.counter = counter;
        counter.leftHandleSet.add(this);
        this.rightHandleSet = rightHandleSet;
        rightHandleSet.add(this);
    }

    public void removeByLeft() {
        rightHandleSet.remove(this); // The counter will be removed from the left handle set by the caller.
    }

    public void removeByRight() {
        counter.leftHandleSet.remove(this); // The counter will be removed from the right handle set by the caller.
    }

}
