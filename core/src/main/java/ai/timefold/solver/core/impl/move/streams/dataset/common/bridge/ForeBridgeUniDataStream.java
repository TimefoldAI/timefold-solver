package ai.timefold.solver.core.impl.move.streams.dataset.common.bridge;

import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DefaultDatasetFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataStream;

public final class ForeBridgeUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements UniDataStream<Solution_, A> {

    public ForeBridgeUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory,
            AbstractDataStream<Solution_> parent) {
        super(datasetFactory, parent);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        // Do nothing. The child stream builds everything.
    }

    @Override
    public String toString() {
        return "Generic bridge";
    }

}
