package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AftBridgeUniEnumeratingStream<Solution_, A>
        extends AbstractUniEnumeratingStream<Solution_, A>
        implements TupleSource {

    public AftBridgeUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractEnumeratingStream<Solution_> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        // Do nothing. The parent stream builds everything.
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AftBridgeUniEnumeratingStream<?, ?> that && Objects.equals(parent, that.parent);
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
