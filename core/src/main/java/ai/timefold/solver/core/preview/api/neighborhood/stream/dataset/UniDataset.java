package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

/**
 * An opaque handle to a dataset registered via {@link MoveStreamFactory#register(UniEnumeratingStream)}.
 * Carries no behavior of its own; resolve it against a {@link MoveIteratorSession}
 * to obtain a {@link UniDatasetInstance}.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
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
