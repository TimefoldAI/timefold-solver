package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.bi.ConcatBiBiNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.ConcatEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeBiEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class BiConcatBiEnumeratingStream<Solution_, A, B> extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements ConcatEnumeratingStream<Solution_> {

    private final ForeBridgeBiEnumeratingStream<Solution_, A, B> leftParent;
    private final ForeBridgeBiEnumeratingStream<Solution_, A, B> rightParent;

    public BiConcatBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            ForeBridgeBiEnumeratingStream<Solution_, A, B> leftParent,
            ForeBridgeBiEnumeratingStream<Solution_, A, B> rightParent) {
        super(enumeratingStreamFactory);
        this.leftParent = leftParent;
        this.rightParent = rightParent;
    }

    @Override
    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        leftParent.collectActiveEnumeratingStreams(enumeratingStreamSet);
        rightParent.collectActiveEnumeratingStreams(enumeratingStreamSet);
        enumeratingStreamSet.add(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        TupleLifecycle<BiTuple<A, B>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        var rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = new ConcatBiBiNode<>(downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    @Override
    public boolean equals(Object o) {
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return o instanceof BiConcatBiEnumeratingStream<?, ?, ?> other
                && Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(BiConcatBiEnumeratingStream.class, leftParent.getParent(), rightParent.getParent());
    }

    @Override
    public String toString() {
        return "BiConcat() with " + childStreamList.size() + " children";
    }

    @Override
    public AbstractEnumeratingStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public AbstractEnumeratingStream<Solution_> getRightParent() {
        return rightParent;
    }

}
