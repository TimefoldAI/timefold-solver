package ai.timefold.solver.core.impl.move.streams.dataset.common.bridge;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DefaultDatasetFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

public final class AftBridgeUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource {

    public AftBridgeUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory, AbstractDataStream<Solution_> parent) {
        super(datasetFactory, parent);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        // Do nothing. The parent stream builds everything.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AftBridgeUniDataStream<?, ?> that = (AftBridgeUniDataStream<?, ?>) o;
        return Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public String toString() {
        return "Bridge from " + parent + " with " + childStreamList.size() + " children";
    }

}
