package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractLeftDataset<Solution_, A> extends AbstractDataset<Solution_, UniTuple<A>> {

    protected AbstractLeftDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(enumeratingStreamFactory, parent);
    }

    public abstract AbstractLeftDatasetInstance<Solution_, UniTuple<A>> instantiate(int entryStoreIndex);

}
