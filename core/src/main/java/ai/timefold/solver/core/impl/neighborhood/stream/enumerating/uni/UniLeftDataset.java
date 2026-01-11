package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniLeftDataset<Solution_, A> extends AbstractLeftDataset<Solution_, A> {

    public UniLeftDataset(AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(parent);
    }

    @Override
    public UniLeftDatasetInstance<Solution_, A> instantiate(int rightSequenceStoreIndex, int entryStoreIndex) {
        return new UniLeftDatasetInstance<>(this, rightSequenceStoreIndex, entryStoreIndex);
    }

    @Override
    public boolean equals(Object compositeKey) {
        return compositeKey instanceof UniLeftDataset<?, ?> other
                && Objects.equals(parent, other.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

}
