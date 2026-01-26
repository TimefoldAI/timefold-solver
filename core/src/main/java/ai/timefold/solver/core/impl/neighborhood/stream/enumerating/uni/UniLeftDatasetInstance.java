package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniLeftDatasetInstance<Solution_, A>
        extends AbstractLeftDatasetInstance<Solution_, UniTuple<A>> {

    public UniLeftDatasetInstance(AbstractDataset<Solution_> parent, int rightIteratorStoreIndex, int entryStoreIndex) {
        super(parent, rightIteratorStoreIndex, entryStoreIndex);
    }

}
