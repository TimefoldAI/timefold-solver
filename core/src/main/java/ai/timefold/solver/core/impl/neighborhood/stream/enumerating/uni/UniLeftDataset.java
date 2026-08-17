package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.JustInTimeBiDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.BiNeighborhoodsJoinerComber;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniLeftDataset<Solution_, A> extends AbstractLeftDataset<Solution_, UniTuple<A>>
        implements UniDataset<Solution_, A> {

    public UniLeftDataset(AbstractUniEnumeratingStream<Solution_, A> parent) {
        super(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other,
            BiNeighborhoodsJoiner<A, B>... joiners) {
        var joinerComber = BiNeighborhoodsJoinerComber.<Solution_, A, B> comb(joiners);
        var rightDataset =
                ((AbstractUniEnumeratingStream<Solution_, B>) other).asCachedDataset(joinerComber);
        return new JustInTimeBiDataset<>(this, rightDataset);
    }

    @Override
    public UniLeftDatasetInstance<Solution_, A> instantiate(int entryStoreIndex) {
        return new UniLeftDatasetInstance<>(this, entryStoreIndex);
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
