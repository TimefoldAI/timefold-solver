package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDataset;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;

/**
 * Accepts two datasets ({@link UniLeftDataset left} and {@link UniRightDataset right}),
 * coming from two enumerating streams,
 * and provides {@link Move} iterators based on the join and filtering of those datasets.
 * The merged iterators provide {@link Move moves} constructed by a {@link BiMoveConstructor move constructor},
 * which accepts instances of type A and B.
 * <p>
 * Moves are produced by a {@link BiRandomMoveIterator},
 * which picks A and B randomly.
 * Move order is never part of the API's contract.
 *
 * @param <Solution_>
 * @param <A>
 * @param <B>
 */
@NullMarked
public final class BiMoveStream<Solution_, A, B> implements InnerMoveStream<Solution_> {

    private final UniLeftDataset<Solution_, A> leftDataset;
    private final UniRightDataset<Solution_, A, B> rightDataset;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;

    public BiMoveStream(UniLeftDataset<Solution_, A> leftDataset, UniRightDataset<Solution_, A, B> rightDataset,
            BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        var context = new BiMoveStreamContext<>((DefaultNeighborhoodSession<Solution_>) neighborhoodSession, leftDataset,
                rightDataset, moveConstructor);
        return new BiMoveIterable<>(context);
    }

    private record BiMoveIterable<Solution_, A, B>(BiMoveStreamContext<Solution_, A, B> context)
            implements
                MoveIterable<Solution_> {

        private BiMoveIterable(BiMoveStreamContext<Solution_, A, B> context) {
            this.context = Objects.requireNonNull(context);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(RandomGenerator random) {
            return new BiRandomMoveIterator<>(context, random);
        }

    }

}
