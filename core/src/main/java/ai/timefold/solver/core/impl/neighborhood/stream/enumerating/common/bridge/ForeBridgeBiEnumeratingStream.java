package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.AbstractBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForeBridgeBiEnumeratingStream<Solution_, A, B>
        extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements BiEnumeratingStream<Solution_, A, B> {

    public ForeBridgeBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractEnumeratingStream<Solution_> parent) {
        super(enumeratingStreamFactory, parent);
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
