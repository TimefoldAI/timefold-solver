package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.joiner.BiEnumeratingJoinerComber;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Accepts two {@link UniDataset datasets} (coming from two enumerating streams),
 * and provides {@link Move} iterators, based on {@link BiEnumeratingJoinerComber joiners and filtering}.
 * The datasets are called "left" and "right"; left provides instances of type A and right of type B.
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

    private final UniDataset<Solution_, A> leftDataset;
    private final UniDataset<Solution_, B> rightDataset;
    private final BiEnumeratingJoinerComber<Solution_, A, B> joinerComber;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;

    public BiMoveStream(UniDataset<Solution_, A> leftDataset, UniDataset<Solution_, B> rightDataset,
            BiEnumeratingJoinerComber<Solution_, A, B> comber, BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.leftDataset = Objects.requireNonNull(leftDataset);
        this.rightDataset = Objects.requireNonNull(rightDataset);
        this.joinerComber = Objects.requireNonNull(comber);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        var context = new BiMoveStreamContext<>((DefaultNeighborhoodSession<Solution_>) neighborhoodSession, leftDataset,
                rightDataset, joinerComber, moveConstructor);
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
        public Iterator<Move<Solution_>> iterator(Random random) {
            return new BiRandomMoveIterator<>(context, random);
        }

    }

}
