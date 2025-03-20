package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStreamFactory<Solution_> {

    // TODO should this include unassigned? It does now.
    <A> UniDataStream<Solution_, A> enumerate(Class<A> clz);

    default <A> UniMoveStream<Solution_, A> pick(Class<A> clz) {
        return pick(enumerate(clz));
    }

    <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream);

}
