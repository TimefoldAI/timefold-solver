package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.TerminalEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class TerminalUniEnumeratingStream<Solution_, A>
        extends AbstractUniEnumeratingStream<Solution_, A>
        implements TerminalEnumeratingStream<Solution_, UniTuple<A>, UniDataset<Solution_, A>> {

    private final UniDataset<Solution_, A> dataset;

    public TerminalUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(enumeratingStreamFactory, parent);
        this.dataset = new UniDataset<>(enumeratingStreamFactory, this);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(inputStoreIndex));
    }

    @Override
    public UniDataset<Solution_, A> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
