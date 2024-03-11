package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractUnindexedJoinNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class UnindexedJoinTriNode<A, B, C>
        extends AbstractUnindexedJoinNode<BiTuple<A, B>, C, TriTuple<A, B, C>> {

    private final TriPredicate<A, B, C> filtering;
    private final int outputStoreSize;

    public UnindexedJoinTriNode(
            int inputStoreIndexLeftEntry, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightEntry, int inputStoreIndexRightOutTupleList,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, TriPredicate<A, B, C> filtering,
            int outputStoreSize,
            int outputStoreIndexLeftOutEntry, int outputStoreIndexRightOutEntry) {
        super(inputStoreIndexLeftEntry, inputStoreIndexLeftOutTupleList,
                inputStoreIndexRightEntry, inputStoreIndexRightOutTupleList,
                nextNodesTupleLifecycle, filtering != null,
                outputStoreIndexLeftOutEntry, outputStoreIndexRightOutEntry);
        this.filtering = filtering;
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> createOutTuple(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, rightTuple.factA, outputStoreSize);
    }

    @Override
    protected void setOutTupleLeftFacts(TriTuple<A, B, C> outTuple, BiTuple<A, B> leftTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void setOutTupleRightFact(TriTuple<A, B, C> outTuple, UniTuple<C> rightTuple) {
        outTuple.factC = rightTuple.factA;
    }

    @Override
    protected boolean testFiltering(BiTuple<A, B> leftTuple, UniTuple<C> rightTuple) {
        return filtering.test(leftTuple.factA, leftTuple.factB, rightTuple.factA);
    }

}
