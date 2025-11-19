package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.IndexedSet;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ExistsCounter<Tuple_ extends AbstractTuple>
        extends AbstractPropagationMetadataCarrier<Tuple_> {

    final Tuple_ leftTuple;
    final IndexedSet<ExistsCounterHandle<Tuple_>> leftHandleSet = new IndexedSet<>(ExistsCounterHandlePositionTracker.left());
    TupleState state = TupleState.DEAD; // It's the node's job to mark a new instance as CREATING.
    int countRight = 0;
    int indexedSetPositon = -1;

    ExistsCounter(Tuple_ leftTuple) {
        this.leftTuple = leftTuple;
    }

    public void clearWithoutCount() {
        leftHandleSet.forEach(ExistsCounterHandle::remove);
    }

    public void clearIncludingCount() {
        clearWithoutCount();
        countRight = 0;
    }

    @Override
    public Tuple_ getTuple() {
        return leftTuple;
    }

    @Override
    public TupleState getState() {
        return state;
    }

    @Override
    public void setState(TupleState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Counter(" + leftTuple + ")";
    }

}
