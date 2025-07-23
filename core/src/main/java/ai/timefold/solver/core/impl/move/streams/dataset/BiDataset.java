package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;

public final class BiDataset<Solution_, A, B> extends AbstractDataset<Solution_, BiTuple<A, B>> {

    public BiDataset(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent) {
        super(dataStreamFactory, parent);
    }

}
