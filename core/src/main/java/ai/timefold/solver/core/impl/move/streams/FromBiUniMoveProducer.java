package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.BiDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

@NullMarked
public final class FromBiUniMoveProducer<Solution_, A, B> implements InnerMoveProducer<Solution_> {

    private final BiDataset<Solution_, A, B> aDataset;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;

    public FromBiUniMoveProducer(BiDataset<Solution_, A, B> aDataset, BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.aDataset = Objects.requireNonNull(aDataset);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(MoveStreamSession<Solution_> moveStreamSession) {
        return new InnerMoveIterable((DefaultMoveStreamSession<Solution_>) moveStreamSession);
    }

    @Override
    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> activeDataStreamSet) {
        aDataset.collectActiveDataStreams(activeDataStreamSet);
    }

    @NullMarked
    private final class InnerMoveIterator implements Iterator<Move<Solution_>> {

        private final IteratorSupplier<A, B> iteratorSupplier;
        private final SolutionView<Solution_> solutionView;

        // Fields required for iteration.
        private @Nullable Move<Solution_> nextMove;
        private @Nullable Iterator<BiTuple<A, B>> iterator;

        public InnerMoveIterator(DefaultMoveStreamSession<Solution_> moveStreamSession) {
            var aInstance = moveStreamSession.getDatasetInstance(aDataset);
            this.iteratorSupplier = aInstance::iterator;
            this.solutionView = moveStreamSession.getSolutionView();
        }

        public InnerMoveIterator(DefaultMoveStreamSession<Solution_> moveStreamSession, Random random) {
            var aInstance = moveStreamSession.getDatasetInstance(aDataset);
            this.iteratorSupplier = () -> aInstance.iterator(random);
            this.solutionView = moveStreamSession.getSolutionView();
        }

        @Override
        public boolean hasNext() {
            // If we already found the next move, return true.
            if (nextMove != null) {
                return true;
            }

            // Initialize iterator if needed.
            if (iterator == null) {
                iterator = iteratorSupplier.get();
            }

            // If iterator is empty, there's no next move.
            if (!iterator.hasNext()) {
                return false;
            }

            var tuple = iterator.next();
            nextMove = moveConstructor.apply(solutionView, tuple.factA, tuple.factB);
            return true;
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = nextMove;
            nextMove = null;
            return result;
        }

        @FunctionalInterface
        private interface IteratorSupplier<A, B> extends Supplier<Iterator<BiTuple<A, B>>> {

        }
    }

    @NullMarked
    private final class InnerMoveIterable implements MoveIterable<Solution_> {

        private final DefaultMoveStreamSession<Solution_> moveStreamSession;

        public InnerMoveIterable(DefaultMoveStreamSession<Solution_> moveStreamSession) {
            this.moveStreamSession = Objects.requireNonNull(moveStreamSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator() {
            return new InnerMoveIterator(moveStreamSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(Random random) {
            return new InnerMoveIterator(moveStreamSession, random);
        }

    }

}
