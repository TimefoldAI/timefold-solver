package ai.timefold.solver.core.impl.bavet.uni;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class IndexedIfExistsUniNode<A, B> extends AbstractIndexedIfExistsNode<UniTuple<A>, B> {

    private final BiPredicate<A, B> filtering;

    public IndexedIfExistsUniNode(boolean shouldExist, IndexerFactory<B> indexerFactory,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        this(shouldExist, indexerFactory, nextNodesTupleLifecycle, null, tupleStorePositionTracker);
    }

    public IndexedIfExistsUniNode(boolean shouldExist, IndexerFactory<B> indexerFactory,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, BiPredicate<A, B> filtering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, indexerFactory.buildUniLeftKeysExtractor(), indexerFactory, nextNodesTupleLifecycle,
                filtering != null, tupleStorePositionTracker);
        this.filtering = filtering;
    }

    @Override
    protected boolean testFiltering(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
        return filtering.test(leftTuple.factA, rightTuple.factA);
    }

}
