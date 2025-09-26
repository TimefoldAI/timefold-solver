package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class FromBiUniMoveStream<Solution_, A, B> implements InnerMoveStream<Solution_> {

    private final BiDataset<Solution_, A, B> aDataset;
    private final BiMoveConstructor<Solution_, A, B> moveConstructor;

    public FromBiUniMoveStream(BiDataset<Solution_, A, B> aDataset, BiMoveConstructor<Solution_, A, B> moveConstructor) {
        this.aDataset = Objects.requireNonNull(aDataset);
        this.moveConstructor = Objects.requireNonNull(moveConstructor);
    }

    @Override
    public MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession) {
        return new InnerMoveIterable((DefaultNeighborhoodSession<Solution_>) neighborhoodSession);
    }

    @Override
    public void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet) {
        aDataset.collectActiveEnumeratingStreams(enumeratingStreamSet);
    }

    @NullMarked
    private final class InnerMoveIterator implements Iterator<Move<Solution_>> {

        private final IteratorSupplier<A, B> iteratorSupplier;
        private final SolutionView<Solution_> solutionView;

        // Fields required for iteration.
        private @Nullable Move<Solution_> nextMove;
        private @Nullable Iterator<BiTuple<A, B>> iterator;

        public InnerMoveIterator(DefaultNeighborhoodSession<Solution_> neighborhoodSession) {
            var aInstance = neighborhoodSession.getDatasetInstance(aDataset);
            this.iteratorSupplier = aInstance::iterator;
            this.solutionView = neighborhoodSession.getSolutionView();
        }

        public InnerMoveIterator(DefaultNeighborhoodSession<Solution_> neighborhoodSession, Random random) {
            var aInstance = neighborhoodSession.getDatasetInstance(aDataset);
            this.iteratorSupplier = () -> aInstance.iterator(random);
            this.solutionView = neighborhoodSession.getSolutionView();
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

        private final DefaultNeighborhoodSession<Solution_> neighborhoodSession;

        public InnerMoveIterable(DefaultNeighborhoodSession<Solution_> neighborhoodSession) {
            this.neighborhoodSession = Objects.requireNonNull(neighborhoodSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator() {
            return new InnerMoveIterator(neighborhoodSession);
        }

        @Override
        public Iterator<Move<Solution_>> iterator(Random random) {
            return new InnerMoveIterator(neighborhoodSession, random);
        }

    }

}
