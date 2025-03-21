package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class UniDataset<Solution_, A> extends AbstractDataset<Solution_, UniTuple<A>> {

    public UniDataset(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parent) {
        super(dataStreamFactory, parent);
    }

}
