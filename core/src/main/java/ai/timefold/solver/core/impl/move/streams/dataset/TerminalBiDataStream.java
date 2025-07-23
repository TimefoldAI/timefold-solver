package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class TerminalBiDataStream<Solution_, A, B>
        extends AbstractBiDataStream<Solution_, A, B> {

    private final BiDataset<Solution_, A, B> dataset;

    public TerminalBiDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent) {
        super(dataStreamFactory, parent);
        this.dataset = new BiDataset<>(dataStreamFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    public BiDataset<Solution_, A, B> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
