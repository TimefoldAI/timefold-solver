package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.api.function.PentaPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedIfExistsQuadNode<A, B, C, D, E> extends AbstractIndexedIfExistsNode<QuadTuple<A, B, C, D>, E> {

    private final PentaPredicate<A, B, C, D, E> filtering;

    public IndexedIfExistsQuadNode(boolean shouldExist, IndexerFactory<E> indexerFactory,
            TupleStorePositionTracker leftTupleStorePositionTracker, TupleStorePositionTracker rightTupleStorePositionTracker,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, PentaPredicate<A, B, C, D, E> filtering) {
        super(shouldExist, indexerFactory.buildQuadLeftKeysExtractor(), indexerFactory, leftTupleStorePositionTracker,
                rightTupleStorePositionTracker, nextNodesTupleLifecycle, filtering != null);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(QuadTuple<A, B, C, D> leftTuple, UniTuple<E> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD,
                rightTuple.factA);
    }

}
