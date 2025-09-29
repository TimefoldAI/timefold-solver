package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BiDataset<Solution_, A, B> extends AbstractDataset<Solution_, BiTuple<A, B>> {

    public BiDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public BiDatasetInstance<Solution_, A, B> instantiate(int storeIndex) {
        return new BiDatasetInstance<>(this, storeIndex);
    }

}
