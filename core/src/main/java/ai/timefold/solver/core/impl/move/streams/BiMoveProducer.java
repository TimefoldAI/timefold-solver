package ai.timefold.solver.core.impl.move.streams;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BiMoveProducer<Solution_, A, B> implements InnerMoveProducer<Solution_> {

    private final AbstractDataset<Solution_, UniTuple<A>> aDataset;
    private final AbstractDataset<Solution_, UniTuple<B>> bDataset;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;
    private final BiPredicate<A, B> filter;

    public BiMoveProducer(AbstractDataset<Solution_, UniTuple<A>> aDataset, AbstractDataset<Solution_, UniTuple<B>> bDataset,
            BiPredicate<A, B> filter, BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.aDataset = Objects.requireNonNull(aDataset);
        this.bDataset = Objects.requireNonNull(bDataset);
        this.filter = Objects.requireNonNull(filter);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(MoveStreamSession<Solution_> moveStreamSession) {
        return new BiMoveIterable((DefaultMoveStreamSession<Solution_>) moveStreamSession);
    }

    @Override
    public void collectActiveDataStreams(Set<AbstractDataStream<Solution_>> activeDataStreamSet) {
        aDataset.collectActiveDataStreams(activeDataStreamSet);
        bDataset.collectActiveDataStreams(activeDataStreamSet);
    }

    private final class BiMoveIterator implements Iterator<Move<Solution_>> {

        private final IteratorSupplier<A> aIteratorSupplier;
        private final IteratorSupplier<B> bIteratorSupplier;
        private final Solution_ solution;

        // Fields required for iteration.
        private @Nullable Move<Solution_> nextMove;
        private @Nullable Iterator<UniTuple<A>> aIterator;
        private @Nullable Iterator<UniTuple<B>> bIterator;
        private @Nullable A currentA;

        public BiMoveIterator(DefaultMoveStreamSession<Solution_> moveStreamSession) {
            var aInstance = moveStreamSession.getDatasetInstance(aDataset);
            this.aIteratorSupplier = aInstance::iterator;
            var bInstance = moveStreamSession.getDatasetInstance(bDataset);
            this.bIteratorSupplier = bInstance::iterator;
            this.solution = moveStreamSession.getWorkingSolution();
        }

        public BiMoveIterator(DefaultMoveStreamSession<Solution_> moveStreamSession, Random random) {
            var aInstance = moveStreamSession.getDatasetInstance(aDataset);
            this.aIteratorSupplier = () -> aInstance.iterator(random);
            var bInstance = moveStreamSession.getDatasetInstance(bDataset);
            this.bIteratorSupplier = () -> bInstance.iterator(random);
            this.solution = moveStreamSession.getWorkingSolution();
        }

        @Override
        public boolean hasNext() {
            // If we already found the next move, return true.
            if (nextMove != null) {
                return true;
            }

            // Initialize iterators if needed.
            if (aIterator == null) {
                aIterator = aIteratorSupplier.get();
                // If first iterator is empty, there's no next move.
                if (!aIterator.hasNext()) {
                    return false;
                }
                currentA = aIterator.next().factA;
                bIterator = bIteratorSupplier.get();
            }

            // Try to find the next valid move.
            while (true) {
                // If inner iterator has more elements...
                while (bIterator.hasNext()) {
                    var bTuple = bIterator.next();
                    var currentB = bTuple.factA;

                    // Check if this pair passes the filter...
                    if (filter.test(currentA, currentB)) {
                        // ... and create the next move.
                        nextMove = moveConstructor.apply(solution, currentA, currentB);
                        return true;
                    }
                }

                // Inner iterator exhausted, move to next outer element.
                if (aIterator.hasNext()) {
                    currentA = aIterator.next().factA;
                    // Reset inner iterator for new outer element.
                    bIterator = bIteratorSupplier.get();
                } else {
                    // Both iterators exhausted.
                    return false;
                }
            }
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
        private interface IteratorSupplier<A> extends Supplier<Iterator<UniTuple<A>>> {

        }
    }

    private final class BiMoveIterable implements MoveIterable<Solution_> {

        private final DefaultMoveStreamSession<Solution_> moveStreamSession;

        public BiMoveIterable(DefaultMoveStreamSession<Solution_> moveStreamSession) {
            this.moveStreamSession = Objects.requireNonNull(moveStreamSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator() {
            return new BiMoveIterator(moveStreamSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(Random random) {
            return new BiMoveIterator(moveStreamSession, random);
        }

    }

}
