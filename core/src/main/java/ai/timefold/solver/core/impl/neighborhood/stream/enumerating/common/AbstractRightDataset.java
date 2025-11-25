package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractRightDataset<Solution_, A, B> extends AbstractDataset<Solution_, UniTuple<B>> {

    protected AbstractRightDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, B> parent) {
        super(enumeratingStreamFactory, parent);
    }

    public abstract AbstractRightDatasetInstance<Solution_, B> instantiate(int compositeKeyStoreIndex, int entryStoreIndex);

}
