package ai.timefold.solver.core.impl.move.streams.dataset.common.bridge;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.uni.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class AftBridgeUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A>
        implements TupleSource {

    public AftBridgeUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractDataStream<Solution_> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        // Do nothing. The parent stream builds everything.
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AftBridgeUniDataStream<?, ?> that && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.requireNonNull(parent).hashCode();
    }

    @Override
    public String toString() {
        return "Bridge from " + parent + " with " + childStreamList.size() + " children";
    }

}
