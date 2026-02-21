package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.ForEachFilteredUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachUnfilteredUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BavetForEachUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements TupleSource {

    private final Class<A> forEachClass;
    @Nullable
    private final Function<ConstraintNodeBuildHelper<Solution_, ?>, Predicate<A>> filterFunction;

    public BavetForEachUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory, Class<A> forEachClass,
            @Nullable Function<ConstraintNodeBuildHelper<Solution_, ?>, Predicate<A>> filterFunction,
            RetrievalSemantics retrievalSemantics) {
        super(constraintFactory, retrievalSemantics);
        this.forEachClass = Objects.requireNonNull(forEachClass);
        this.filterFunction = filterFunction;
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
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        TupleLifecycle<UniTuple<A>> tupleLifecycle = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        int outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var filter = filterFunction != null ? filterFunction.apply(buildHelper) : null;
        var node = filter == null ? new ForEachUnfilteredUniNode<>(forEachClass, tupleLifecycle, outputStoreSize)
                : new ForEachFilteredUniNode<>(forEachClass, filter, tupleLifecycle,
                        outputStoreSize);
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
        return Objects.equals(forEachClass, that.forEachClass) && Objects.equals(filterFunction, that.filterFunction)
                && getRetrievalSemantics().equals(that.getRetrievalSemantics());
    }

    @Override
    public int hashCode() {
        return Objects.hash(forEachClass, filterFunction, getRetrievalSemantics());
    }

    @Override
    public String toString() {
        if (filterFunction != null) {
            return "ForEach(" + forEachClass.getSimpleName() + ") with filter and " + childStreamList.size() + " children";
        }
        return "ForEach(" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
