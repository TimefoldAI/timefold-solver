package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.uni.ForEachExcludingPinnedUniNode;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachExcludingPinnedDataStream<Solution_, A>
        extends AbstractForEachDataStream<Solution_, A>
        implements TupleSource {

    private final PlanningEntityMetaModel<Solution_, A> entityMetaModel;

    public ForEachExcludingPinnedDataStream(DataStreamFactory<Solution_> dataStreamFactory,
            PlanningEntityMetaModel<Solution_, A> entityMetaModel) {
        super(dataStreamFactory, Objects.requireNonNull(entityMetaModel).type());
        this.entityMetaModel = entityMetaModel;
    }

    @Override
    protected AbstractForEachUniNode<A> getNode(TupleLifecycle<UniTuple<A>> tupleLifecycle, int outputStoreSize) {
        return new ForEachExcludingPinnedUniNode<>(entityMetaModel, tupleLifecycle, outputStoreSize);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForEachExcludingPinnedDataStream<?, ?> that &&
                Objects.equals(forEachClass, that.forEachClass);
    }

    @Override
    public int hashCode() {
        return forEachClass.hashCode();
    }

    @Override
    public String toString() {
        return "ForEach excluding pinned (" + forEachClass.getSimpleName() + ") with " + childStreamList.size() + " children";
    }

}
