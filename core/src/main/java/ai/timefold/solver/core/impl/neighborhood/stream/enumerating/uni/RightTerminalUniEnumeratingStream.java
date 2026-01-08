package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.neighborhood.joiner.BiNeighborhoodsJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.TerminalEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class RightTerminalUniEnumeratingStream<Solution_, A, B>
        extends AbstractUniEnumeratingStream<Solution_, B>
        implements TerminalEnumeratingStream<Solution_, UniRightDataset<Solution_, A, B>> {

    private final UniRightDataset<Solution_, A, B> dataset;

    public RightTerminalUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, B> parent, BiNeighborhoodsJoinerComber<Solution_, A, B> joinerComber) {
        super(enumeratingStreamFactory, parent);
        this.dataset = new UniRightDataset<>(this, joinerComber);
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        var compositeKeyStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        var inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        buildHelper.putInsertUpdateRetract(this, dataset.instantiate(compositeKeyStoreIndex, inputStoreIndex));
    }

    @Override
    public UniRightDataset<Solution_, A, B> getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        return "Terminal node";
    }

}
