package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDataset;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodSession;

import org.jspecify.annotations.NullMarked;

/**
 * Accepts two datasets ({@link UniLeftDataset left} and {@link UniRightDataset right}),
 * coming from two enumerating streams,
 * and provides {@link Move} iterators based on the join and filtering of those datasets.
 * The merged iterators provide {@link Move moves} constructed by a {@link BiMoveConstructor move constructor},
 * which accepts instances of type A and B.
 * 
 * <p>
 * There are two types of iterators:
 *
 * <ul>
 * <li>{@link BiOriginalMoveIterator Original order iterators},
 * which iterate through all possible combinations of A and B in the original order.</li>
 * <li>{@link BiRandomMoveIterator Random order iterators},
 * which pick A and B randomly.</li>
 * </ul>
 *
 * Please refer to the respective iterator classes for documentation on their strategies.
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
        public Iterator<Move<Solution_>> iterator() {
            return new BiOriginalMoveIterator<>(context);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(RandomGenerator random) {
            return new BiRandomMoveIterator<>(context, random);
        }

    }

}
