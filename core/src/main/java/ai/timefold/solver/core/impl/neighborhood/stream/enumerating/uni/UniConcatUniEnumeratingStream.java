package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.ConcatUniUniNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.ConcatEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class UniConcatUniEnumeratingStream<Solution_, A> extends AbstractUniEnumeratingStream<Solution_, A>
        implements ConcatEnumeratingStream<Solution_> {

    private final ForeBridgeUniEnumeratingStream<Solution_, A> leftParent;
    private final ForeBridgeUniEnumeratingStream<Solution_, A> rightParent;

    public UniConcatUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            ForeBridgeUniEnumeratingStream<Solution_, A> leftParent, ForeBridgeUniEnumeratingStream<Solution_, A> rightParent) {
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
        TupleLifecycle<UniTuple<A>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        var rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = new ConcatUniUniNode<>(downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    @Override
    public boolean equals(Object o) {
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return o instanceof UniConcatUniEnumeratingStream<?, ?> other
                && Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(UniConcatUniEnumeratingStream.class, leftParent.getParent(), rightParent.getParent());
    }

    @Override
    public String toString() {
        return "UniConcat() with " + childStreamList.size() + " children";
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
