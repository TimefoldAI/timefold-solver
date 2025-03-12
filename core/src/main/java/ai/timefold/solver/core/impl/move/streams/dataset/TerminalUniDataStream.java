package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;

final class TerminalUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final AbstractDataset<Solution_, UniTuple<A>> dataset;

    public TerminalUniDataStream(DefaultDataStreamFactory<Solution_> dataStreamFactory,
            AbstractUniDataStream<Solution_, A> parent) {
        super(dataStreamFactory, parent);
        this.dataset = new UniDataset<>(dataStreamFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent);
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    public AbstractDataset<Solution_, UniTuple<A>> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
