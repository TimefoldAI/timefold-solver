package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractUnindexedIfExistsNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.api.function.QuadPredicate;

final class UnindexedIfExistsTriNode<A, B, C, D> extends AbstractUnindexedIfExistsNode<TriTuple<A, B, C>, D> {

    private final QuadPredicate<A, B, C, D> filtering;

    public UnindexedIfExistsTriNode(boolean shouldExist,
            int inputStoreIndexLeftPropagationQueueMetadata, int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightEntry,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle) {
        this(shouldExist,
                inputStoreIndexLeftPropagationQueueMetadata, inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public UnindexedIfExistsTriNode(boolean shouldExist,
            int inputStoreIndexLeftPropagationQueueMetadata, int inputStoreIndexLeftCounterEntry,
            int inputStoreIndexLeftTrackerList, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            QuadPredicate<A, B, C, D> filtering) {
        super(shouldExist,
                inputStoreIndexLeftPropagationQueueMetadata, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(TriTuple<A, B, C> leftTuple, UniTuple<D> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, rightTuple.factA);
    }

}
