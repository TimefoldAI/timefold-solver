package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class BavetFilterUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final Predicate<A> predicate;

    public BavetFilterUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, Predicate<A> predicate) {
        super(constraintFactory, parent);
        this.predicate = predicate;
        if (predicate == null) {
            throw new IllegalArgumentException("The predicate (null) cannot be null.");
        }
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        buildHelper.<UniTuple<A>> putInsertUpdateRetract(this, childStreamList,
                tupleLifecycle -> new ConditionalUniTupleLifecycle<>(predicate, tupleLifecycle));
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public int hashCode() {
        return Objects.hash(parent, predicate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetFilterUniConstraintStream other) {
            return parent == other.parent
                    && predicate == other.predicate;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Filter() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
