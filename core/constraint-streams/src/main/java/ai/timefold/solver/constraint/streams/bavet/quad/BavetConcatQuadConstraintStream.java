package ai.timefold.solver.constraint.streams.bavet.quad;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.bi.BavetBiQuadConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeTriConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetForeBridgeUniConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.uni.BavetUniQuadConcatNode;
import ai.timefold.solver.core.api.score.Score;

public final class BavetConcatQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements BavetConcatConstraintStream<Solution_> {

    private final BavetAbstractConstraintStream<Solution_> leftParent;
    private final BavetAbstractConstraintStream<Solution_> rightParent;
    private final ConcatNodeConstructor<A, B, C, D> nodeConstructor;

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeUniConstraintStream<Solution_, A> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetUniQuadConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetBiQuadConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeTriConstraintStream<Solution_, A, B, C> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetQuadTriConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeUniConstraintStream<Solution_, A> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetQuadUniConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetQuadBiConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeTriConstraintStream<Solution_, A, B, C> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetQuadTriConcatNode::new;
    }

    public BavetConcatQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> leftParent,
            BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D> rightParent) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.nodeConstructor = BavetQuadQuadConcatNode::new;
    }

    @Override
    public boolean guaranteesDistinct() {
        if (leftParent instanceof BavetAbstractQuadConstraintStream<Solution_, ?, ?, ?, ?>
                && rightParent instanceof BavetAbstractQuadConstraintStream<Solution_, ?, ?, ?, ?>) {
            // The two parents could have the same source; guarantee impossible.
            return false;
        }
        /*
         * Since one of the two parents is increasing in cardinality,
         * it means its tuples must be distinct from the other parent's tuples.
         * Therefore, the guarantee can be given is both of the parents give it.
         */
        return leftParent.guaranteesDistinct() && rightParent.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        leftParent.collectActiveConstraintStreams(constraintStreamSet);
        rightParent.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<QuadTuple<A, B, C, D>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        int leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        int rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = nodeConstructor.apply(downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BavetConcatQuadConstraintStream<?, ?, ?, ?, ?> other = (BavetConcatQuadConstraintStream<?, ?, ?, ?, ?>) o;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftParent.getParent(), rightParent.getParent());
    }

    @Override
    public String toString() {
        return "Concat() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetAbstractConstraintStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return rightParent;
    }

    private interface ConcatNodeConstructor<A, B, C, D> {

        AbstractConcatNode<?, ?, ?> apply(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
                int leftCloneStoreIndex, int rightCloneStoreIndex, int outputStoreSize);

    }

}
