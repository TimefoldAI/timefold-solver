package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodSession;
import ai.timefold.solver.core.preview.api.neighborhood.UniMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;

/**
 * Accepts a single dataset coming from one enumerating stream,
 * and provides {@link Move} iterators based on that dataset.
 * The iterators provide {@link Move moves} constructed by a {@link UniMoveConstructor move constructor},
 * which accepts instances of type A.
 * <p>
 * Moves are produced by a {@link UniRandomMoveIterator}, which picks A randomly.
 * Move order is never part of the API's contract.
 *
 * @param <Solution_>
 * @param <A>
 */
@NullMarked
public final class UniMoveStream<Solution_, A> implements InnerMoveStream<Solution_> {

    private final UniLeftDataset<Solution_, A> dataset;
    private final UniMoveConstructor<Solution_, A> moveConstructor;

    public UniMoveStream(UniLeftDataset<Solution_, A> dataset, UniMoveConstructor<Solution_, A> moveConstructor) {
        this.dataset = Objects.requireNonNull(dataset);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        var context = new UniMoveStreamContext<>((DefaultNeighborhoodSession<Solution_>) neighborhoodSession, dataset,
                moveConstructor);
        return new UniMoveIterable<>(context);
    }

    private record UniMoveIterable<Solution_, A>(UniMoveStreamContext<Solution_, A> context)
            implements
                MoveIterable<Solution_> {

        private UniMoveIterable(UniMoveStreamContext<Solution_, A> context) {
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(RandomGenerator random) {
            return new UniRandomMoveIterator<>(context, random);
        }

    }

}
