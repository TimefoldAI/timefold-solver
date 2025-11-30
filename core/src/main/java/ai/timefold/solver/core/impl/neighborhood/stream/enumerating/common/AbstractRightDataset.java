package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractRightDataset<Solution_, B> extends AbstractDataset<Solution_> {

    protected AbstractRightDataset(AbstractUniEnumeratingStream<Solution_, B> parent) {
        super(parent);
    }

    public abstract AbstractRightDatasetInstance<Solution_, B> instantiate(int compositeKeyStoreIndex, int entryStoreIndex);

}
