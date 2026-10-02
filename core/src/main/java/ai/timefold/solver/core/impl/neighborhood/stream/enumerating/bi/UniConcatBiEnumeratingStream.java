package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.bi.ConcatBiUniNode;
import ai.timefold.solver.core.impl.bavet.bi.ConcatUniBiNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.ConcatEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.ForeBridgeUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

/**
 * Covers both {@code Uni.concat(Bi, paddingFunction)} and {@code Bi.concat(Uni, paddingFunction)}: same underlying
 * {@link AbstractConcatNode}, selected via {@link ConcatNodeConstructor} depending on which side is the {@code Uni}.
 */
@NullMarked
public final class UniConcatBiEnumeratingStream<Solution_, A, B> extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements ConcatEnumeratingStream<Solution_> {

    private final AbstractEnumeratingStream<Solution_> leftParent;
    private final AbstractEnumeratingStream<Solution_> rightParent;
    private final Function<A, B> paddingFunction;
    private final ConcatNodeConstructor<A, B> nodeConstructor;

    public UniConcatBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            ForeBridgeUniEnumeratingStream<Solution_, A> leftParent,
            ForeBridgeBiEnumeratingStream<Solution_, A, B> rightParent,
            Function<A, B> paddingFunction) {
        super(enumeratingStreamFactory);
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunction = paddingFunction;
        this.nodeConstructor = ConcatUniBiNode::new;
    }

    public UniConcatBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            ForeBridgeBiEnumeratingStream<Solution_, A, B> leftParent,
            ForeBridgeUniEnumeratingStream<Solution_, A> rightParent,
            Function<A, B> paddingFunction) {
        super(enumeratingStreamFactory);
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunction = paddingFunction;
        this.nodeConstructor = ConcatBiUniNode::new;
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
        var node =
                nodeConstructor.apply(paddingFunction, downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    @Override
    public boolean equals(Object o) {
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return o instanceof UniConcatBiEnumeratingStream<?, ?, ?> other
                && Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent())
                && Objects.equals(paddingFunction, other.paddingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(UniConcatBiEnumeratingStream.class, leftParent.getParent(), rightParent.getParent(),
                paddingFunction);
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

    private interface ConcatNodeConstructor<A, B> {

        AbstractConcatNode<?, ?, ?> apply(Function<A, B> paddingFunction,
                TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
                int leftCloneStoreIndex, int rightCloneStoreIndex, int outputStoreSize);

    }

}
