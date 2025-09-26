package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.TerminalEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class TerminalBiEnumeratingStream<Solution_, A, B>
        extends AbstractBiEnumeratingStream<Solution_, A, B>
        implements TerminalEnumeratingStream<Solution_, BiTuple<A, B>, BiDataset<Solution_, A, B>> {

    private final BiDataset<Solution_, A, B> dataset;

    public TerminalBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent) {
        super(enumeratingStreamFactory, parent);
        this.dataset = new BiDataset<>(enumeratingStreamFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    @Override
    public BiDataset<Solution_, A, B> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
