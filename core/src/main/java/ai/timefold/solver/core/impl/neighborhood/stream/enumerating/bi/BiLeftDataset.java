package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BiLeftDataset<Solution_, A, B> extends AbstractLeftDataset<Solution_, BiTuple<A, B>>
        implements BiDataset<Solution_, A, B> {

    public BiLeftDataset(AbstractBiEnumeratingStream<Solution_, A, B> parent) {
        super(parent);
    }

    @Override
    public BiLeftDatasetInstance<Solution_, A, B> instantiate(int entryStoreIndex) {
        return new BiLeftDatasetInstance<>(this, entryStoreIndex);
    }

    @Override
    public boolean equals(Object compositeKey) {
        return compositeKey instanceof BiLeftDataset<?, ?, ?> other
                && Objects.equals(parent, other.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

}
