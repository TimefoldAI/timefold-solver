package ai.timefold.solver.core.impl.move.dataset;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.dataset.common.DataNodeBuildHelper;

final class TerminalUniDataStream<Solution_, A>
        extends AbstractUniDataStream<Solution_, A> {

    private final Dataset<Solution_, UniTuple<A>> dataset;

    public TerminalUniDataStream(DefaultDatasetFactory<Solution_> datasetFactory, AbstractUniDataStream<Solution_, A> parent) {
        super(datasetFactory, parent);
        this.dataset = new Dataset<>(datasetFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(this);
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    public Dataset<Solution_, UniTuple<A>> getDataset() {
        return dataset;
    }

    @Override
    public boolean equals(Object entity) {
        if (!(entity instanceof TerminalUniDataStream<?, ?> that)) {
            return false;
        }
        return Objects.equals(dataset, that.dataset);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataset);
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
