package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.TerminalEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class LeftTerminalBiEnumeratingStream<Solution_, A, B>
        extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements TerminalEnumeratingStream<Solution_, BiLeftDataset<Solution_, A, B>> {

    private final BiLeftDataset<Solution_, A, B> dataset;

    public LeftTerminalBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent) {
        super(enumeratingStreamFactory, parent);
        this.dataset = new BiLeftDataset<>(this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var datasetInstance = dataset.instantiate(buildHelper.reserveTupleStoreIndex(parent.getTupleSource()));
        buildHelper.putInsertUpdateRetract(this, datasetInstance);
    }

    @Override
    public BiLeftDataset<Solution_, A, B> getDataset() {
        return dataset;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LeftTerminalBiEnumeratingStream<?, ?, ?> other
                && parent == other.parent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(LeftTerminalBiEnumeratingStream.class, parent);
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
