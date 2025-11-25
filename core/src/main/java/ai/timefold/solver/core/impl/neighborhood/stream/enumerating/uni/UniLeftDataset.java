package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniLeftDataset<Solution_, A> extends AbstractLeftDataset<Solution_, A> {

    public UniLeftDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public UniLeftDatasetInstance<Solution_, A> instantiate(int entryStoreIndex) {
        return new UniLeftDatasetInstance<>(this, entryStoreIndex);
    }

}
