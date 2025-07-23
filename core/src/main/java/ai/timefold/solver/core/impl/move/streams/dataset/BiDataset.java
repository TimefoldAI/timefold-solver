package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BiDataset<Solution_, A, B> extends AbstractDataset<Solution_, BiTuple<A, B>> {

    public BiDataset(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent) {
        super(dataStreamFactory, parent);
    }

    @Override
    public BiDatasetInstance<Solution_, A, B> instantiate(int storeIndex) {
        return new BiDatasetInstance<>(this, storeIndex);
    }

}
