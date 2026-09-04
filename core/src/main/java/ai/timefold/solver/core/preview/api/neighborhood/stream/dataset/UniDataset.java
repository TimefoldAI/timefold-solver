package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

/**
 * An opaque handle to a dataset created via {@link UniEnumeratingStream#asCachedDataset()}.
 * Carries no behavior of its own; resolve it against a {@link MoveIteratorSession}
 * to obtain a {@link UniDatasetInstance}.
 *
 * @param <Solution_> the solution type
 * @param <A> the type of the dataset's rows
 */
@NullMarked
public interface UniDataset<Solution_, A> {

    @SuppressWarnings("unchecked")
    default <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other) {
        return join(other, new BiNeighborhoodsJoiner[0]);
    }

    @SuppressWarnings("unchecked")
    default <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return join(other, new BiNeighborhoodsJoiner[] { joiner });
    }

    @SuppressWarnings("unchecked")
    default <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2) {
        return join(other, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    @SuppressWarnings("unchecked")
    default <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return join(other, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * Joins this dataset with another enumerating stream, without materializing the join in bavet;
     * the join is instead computed just in time, inside the {@link BiDatasetInstance} it produces.
     *
     * @param other never null
     * @param joiners never null
     * @return never null
     */
    <B> BiDataset<Solution_, A, B> join(UniEnumeratingStream<Solution_, B> other, BiNeighborhoodsJoiner<A, B>... joiners);

}
