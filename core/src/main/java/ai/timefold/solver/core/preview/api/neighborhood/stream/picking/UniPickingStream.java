package ai.timefold.solver.core.preview.api.neighborhood.stream.picking;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.UniMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

/**
 * A single-value variant of {@link PickingStream}.
 *
 * @param <Solution_>
 * @param <A>
 */
@NullMarked
public interface UniPickingStream<Solution_, A> extends PickingStream {

    /**
     * A just-in-time join of two enumerating streams,
     * without any restrictions.
     *
     * @param uniEnumeratingStream stream to join this stream with
     * @return the joined stream, which is lazily evaluated when iterated over
     * @param <B>
     */
    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[0]);
    }

    /**
     * As defined by {@link UniPickingStream#pick(UniEnumeratingStream, BiNeighborhoodsJoiner...)}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    /**
     * As defined by {@link UniPickingStream#pick(UniEnumeratingStream, BiNeighborhoodsJoiner...)}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    /**
     * As defined by {@link UniPickingStream#pick(UniEnumeratingStream, BiNeighborhoodsJoiner...)}.
     */
    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    /**
     * A just-in-time join of two enumerating streams,
     * where only the values that match the joiners will be matched.
     *
     * @param uniEnumeratingStream stream to join this stream with
     * @return the joined stream, which is lazily evaluated when iterated over
     * @param <B>
     */
    <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

    /**
     * Converts this stream into a {@link MoveStream},
     * which will produce a {@link Move} for each value in this stream.
     *
     * @param moveConstructor move constructor to use; see {@link Moves}.
     * @return the move stream, which is lazily evaluated when iterated over
     */
    MoveStream<Solution_> asMove(UniMoveConstructor<Solution_, A> moveConstructor);

}
