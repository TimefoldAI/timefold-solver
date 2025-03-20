package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class TerminalUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final UniDataset<Solution_, A> dataset;

    public TerminalUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parent) {
        super(dataStreamFactory, parent);
        this.dataset = new UniDataset<>(dataStreamFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    public UniDataset<Solution_, A> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
