package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BiLeftDatasetInstance<Solution_, A, B>
        extends AbstractLeftDatasetInstance<Solution_, BiTuple<A, B>> {

    public BiLeftDatasetInstance(AbstractDataset<Solution_> parent, int entryStoreIndex) {
        super(parent, entryStoreIndex);
    }

}
