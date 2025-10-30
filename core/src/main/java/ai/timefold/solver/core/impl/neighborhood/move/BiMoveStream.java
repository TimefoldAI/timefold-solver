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
