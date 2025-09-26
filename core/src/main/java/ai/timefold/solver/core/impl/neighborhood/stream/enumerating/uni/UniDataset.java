package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniDataset<Solution_, A> extends AbstractDataset<Solution_, UniTuple<A>> {

    public UniDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(enumeratingStreamFactory, parent);
    }

    @Override
    public UniDatasetInstance<Solution_, A> instantiate(int storeIndex) {
        return new UniDatasetInstance<>(this, storeIndex);
    }

}
