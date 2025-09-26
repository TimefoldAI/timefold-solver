package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniDataset;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class FromUniBiMoveStream<Solution_, A, B> implements InnerMoveStream<Solution_> {

    private final UniDataset<Solution_, A> aDataset;
    private final UniDataset<Solution_, B> bDataset;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;
    private final BiPredicate<A, B> filter;

    public FromUniBiMoveStream(UniDataset<Solution_, A> aDataset, UniDataset<Solution_, B> bDataset, BiPredicate<A, B> filter,
            BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.aDataset = Objects.requireNonNull(aDataset);
        this.bDataset = Objects.requireNonNull(bDataset);
        this.filter = Objects.requireNonNull(filter);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        return new BiMoveIterable((DefaultNeighborhoodSession<Solution_>) neighborhoodSession);
    }

    @Override
    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        aDataset.collectActiveEnumeratingStreams(enumeratingStreamSet);
        bDataset.collectActiveEnumeratingStreams(enumeratingStreamSet);
    }

    private final class BiMoveIterator implements Iterator<Move<Solution_>> {

        private final IteratorSupplier<A> aIteratorSupplier;
        private final IteratorSupplier<B> bIteratorSupplier;
        private final SolutionView<Solution_> solutionView;

        // Fields required for iteration.
        private @Nullable Move<Solution_> nextMove;
        private @Nullable Iterator<UniTuple<A>> aIterator;
        private @Nullable Iterator<UniTuple<B>> bIterator;
        private @Nullable A currentA;

        public BiMoveIterator(DefaultNeighborhoodSession<Solution_> neighborhoodSession) {
            var aInstance = neighborhoodSession.getDatasetInstance(aDataset);
            this.aIteratorSupplier = aInstance::iterator;
            var bInstance = neighborhoodSession.getDatasetInstance(bDataset);
            this.bIteratorSupplier = bInstance::iterator;
            this.solutionView = neighborhoodSession.getSolutionView();
        }

        public BiMoveIterator(DefaultNeighborhoodSession<Solution_> neighborhoodSession, Random random) {
            var aInstance = neighborhoodSession.getDatasetInstance(aDataset);
            this.aIteratorSupplier = () -> aInstance.iterator(random);
            var bInstance = neighborhoodSession.getDatasetInstance(bDataset);
            this.bIteratorSupplier = () -> bInstance.iterator(random);
            this.solutionView = neighborhoodSession.getSolutionView();
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
                        nextMove = moveConstructor.apply(solutionView, currentA, currentB);
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

        private final DefaultNeighborhoodSession<Solution_> neighborhoodSession;

        public BiMoveIterable(DefaultNeighborhoodSession<Solution_> neighborhoodSession) {
            this.neighborhoodSession = Objects.requireNonNull(neighborhoodSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator() {
            return new BiMoveIterator(neighborhoodSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(Random random) {
            return new BiMoveIterator(neighborhoodSession, random);
        }

    }

}
