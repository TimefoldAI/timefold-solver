package ai.timefold.solver.core.impl.move.streams.dataset.common.bridge;

import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForeBridgeUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements UniDataStream<Solution_, A> {

    public ForeBridgeUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
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
