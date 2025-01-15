package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedIfExistsQuadNode<A, B, C, D, E> extends AbstractIndexedIfExistsNode<QuadTuple<A, B, C, D>, E> {

    private final PentaPredicate<A, B, C, D, E> filtering;

    public IndexedIfExistsQuadNode(boolean shouldExist, IndexFactory<E> indexFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle) {
        this(shouldExist, indexFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, -1,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, null);
    }

    public IndexedIfExistsQuadNode(boolean shouldExist, IndexFactory<E> indexFactory,
            int inputStoreIndexLeftKeys, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightKeys, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, PentaPredicate<A, B, C, D, E> filtering) {
        super(shouldExist, indexFactory.buildQuadLeftKeysExtractor(), indexFactory,
                inputStoreIndexLeftKeys, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightKeys, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> leftTuple, UniTuple<E> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD,
                rightTuple.factA);
    }

}
