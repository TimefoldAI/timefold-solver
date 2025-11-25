package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniRightDataset<Solution_, A, B> extends AbstractRightDataset<Solution_, A, B> {

    private final BiEnumeratingJoinerComber<Solution_, A, B> joinerComber;

    public UniRightDataset(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, B> parent, BiEnumeratingJoinerComber<Solution_, A, B> joinerComber) {
        super(enumeratingStreamFactory, parent);
        this.joinerComber = joinerComber;
    }

    @Override
    public UniRightDatasetInstance<Solution_, A, B> instantiate(int compositeKeyStoreIndex, int entryStoreIndex) {
        var indexerFactory = new IndexerFactory<>(joinerComber.mergedJoiner());
        return new UniRightDatasetInstance<>(this, indexerFactory, joinerComber.mergedFiltering(), compositeKeyStoreIndex,
                entryStoreIndex);
    }
}
