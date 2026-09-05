package ai.timefold.solver.core.preview.api.neighborhood.stream.picking;

import ai.timefold.solver.core.preview.api.neighborhood.UniMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniPickingStream<Solution_, A> extends PickingStream {

    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[0]);
    }

    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner });
    }

    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2 });
    }

    @SuppressWarnings("unchecked")
    default <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B> joiner1, BiNeighborhoodsJoiner<A, B> joiner2, BiNeighborhoodsJoiner<A, B> joiner3) {
        return pick(uniEnumeratingStream, new BiNeighborhoodsJoiner[] { joiner1, joiner2, joiner3 });
    }

    <B> BiPickingStream<Solution_, A, B> pick(UniEnumeratingStream<Solution_, B> uniEnumeratingStream,
            BiNeighborhoodsJoiner<A, B>... joiners);

    MoveStream<Solution_> asMove(UniMoveConstructor<Solution_, A> moveConstructor);

}
