package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIndexedIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ExistsCounter;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.IndexProperties;
import ai.timefold.solver.core.impl.score.stream.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class IndexedIfExistsBiNode<A, B, C> extends AbstractIndexedIfExistsNode<BiTuple<A, B>, C> {

    private final BiFunction<A, B, IndexProperties> mappingAB;
    private final TriPredicate<A, B, C> filtering;

    public IndexedIfExistsBiNode(boolean shouldExist,
            BiFunction<A, B, IndexProperties> mappingAB, Function<C, IndexProperties> mappingC,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexRightProperties,
            int inputStoreIndexRightEntry,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<BiTuple<A, B>>> indexerAB, Indexer<UniTuple<C>> indexerC) {
        this(shouldExist, mappingAB, mappingC,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, -1, inputStoreIndexRightProperties,
                inputStoreIndexRightEntry, -1,
                nextNodesTupleLifecycle, indexerAB, indexerC,
                null);
    }

    public IndexedIfExistsBiNode(boolean shouldExist,
            BiFunction<A, B, IndexProperties> mappingAB, Function<C, IndexProperties> mappingC,
            int inputStoreIndexLeftProperties, int inputStoreIndexLeftCounterEntry, int inputStoreIndexLeftTrackerList,
            int inputStoreIndexRightProperties, int inputStoreIndexRightEntry, int inputStoreIndexRightTrackerList,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            Indexer<ExistsCounter<BiTuple<A, B>>> indexerAB, Indexer<UniTuple<C>> indexerC,
            TriPredicate<A, B, C> filtering) {
        super(shouldExist, mappingC,
                inputStoreIndexLeftProperties, inputStoreIndexLeftCounterEntry, inputStoreIndexLeftTrackerList,
                inputStoreIndexRightProperties, inputStoreIndexRightEntry, inputStoreIndexRightTrackerList,
                nextNodesTupleLifecycle, indexerAB, indexerC,
                filtering != null);
        this.mappingAB = mappingAB;
        this.filtering = filtering;
    }

    @Override
    protected IndexProperties createIndexProperties(BiTuple<A, B> leftTuple) {
        return mappingAB.apply(leftTuple.factA, leftTuple.factB);
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, rightTuple.factA);
    }

}
