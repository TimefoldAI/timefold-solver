package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractRightDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.BiNeighborhoodsJoinerComber;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniRightDataset<Solution_, A, B> extends AbstractRightDataset<Solution_, B> {

    private final BiNeighborhoodsJoinerComber<Solution_, A, B> joinerComber;

    public UniRightDataset(AbstractUniEnumeratingStream<Solution_, B> parent,
            BiNeighborhoodsJoinerComber<Solution_, A, B> joinerComber) {
        super(parent);
        this.joinerComber = joinerComber;
    }

    @Override
    public UniRightDatasetInstance<Solution_, A, B> instantiate(int compositeKeyStoreIndex, int entryStoreIndex) {
        var indexerFactory = new IndexerFactory<>(joinerComber.mergedJoiner());
        return new UniRightDatasetInstance<>(this, indexerFactory, joinerComber.mergedFiltering(), compositeKeyStoreIndex,
                entryStoreIndex);
    }

    @Override
    public boolean equals(Object compositeKey) {
        return compositeKey instanceof UniRightDataset<?, ?, ?> other
                && Objects.equals(joinerComber, other.joinerComber)
                && Objects.equals(parent, other.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(joinerComber, parent);
    }

}
