package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class IteratorMoveStream<Solution_> implements InnerMoveStream<Solution_> {

    private final MoveIteratorProvider<Solution_> iteratorProvider;

    public IteratorMoveStream(MoveIteratorProvider<Solution_> iteratorProvider) {
        this.iteratorProvider = Objects.requireNonNull(iteratorProvider);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        var session = (DefaultNeighborhoodSession<Solution_>) neighborhoodSession;
        return random -> iteratorProvider.iterator(session, random);
    }

}
