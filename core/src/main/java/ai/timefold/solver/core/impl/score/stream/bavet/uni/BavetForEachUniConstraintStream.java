package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

public final class BavetForEachUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements TupleSource {

    private final Class<A> forEachClass;
    private final Predicate<A> filter;

    public BavetForEachUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory, Class<A> forEachClass,
            Predicate<A> filter, RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
        this.forEachClass = forEachClass;
        if (forEachClass == null) {
            throw new IllegalArgumentException("The forEachClass (null) cannot be null.");
        }
        this.filter = filter;
    }

    @Override
    public boolean guaranteesDistinct() {
        return true;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        constraintStreamSet.add(this);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        TupleLifecycle<UniTuple<A>> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node = filter == null ? new ForEachIncludingUnassignedUniNode<>(forEachClass, tupleLifecycle, outputStoreSize)
                : new ForEachExcludingUnassignedUniNode<>(forEachClass, filter, tupleLifecycle, outputStoreSize);
        buildHelper.addNode(node, this, null);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BavetForEachUniConstraintStream<?, ?> that = (BavetForEachUniConstraintStream<?, ?>) other;
        return Objects.equals(forEachClass, that.forEachClass) && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, filter);
    }

    @Override
    public String toString() {
        if (filter != null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") with filter and " + childStreamList.size() + " children";
        }
        return "ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
