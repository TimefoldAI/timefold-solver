package ai.timefold.solver.core.impl.move.dataset;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.dataset.common.DataNodeBuildHelper;

final class TerminalUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final AbstractDataset<Solution_, UniTuple<A>> dataset;

    public TerminalUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory, AbstractUniDataStream<Solution_, A> parent) {
        super(datasetFactory, parent);
        this.dataset = new UniDataset<>(datasetFactory, this);
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
