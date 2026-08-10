package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDatasetInstance;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiIterator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BiDatasetInstance} produced by {@code UniDataset.join}: the join is not materialized in bavet,
 * but computed just in time, inside this instance, out of a left and a right {@code UniDataset}.
 */
@NullMarked
public final class JustInTimeBiDatasetInstance<Solution_, A, B> implements BiDatasetInstance<A, B> {

    private final AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance;
    private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;
    private final SolutionView<Solution_> solutionView;

    public JustInTimeBiDatasetInstance(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance,
            UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance, SolutionView<Solution_> solutionView) {
        this.leftDatasetInstance = Objects.requireNonNull(leftDatasetInstance);
        this.rightDatasetInstance = Objects.requireNonNull(rightDatasetInstance);
        this.solutionView = Objects.requireNonNull(solutionView);
    }

    @Override
    public int size() {
        // ponytail: O(n) sum of indexer lookups; upper bound, filtering() joiners not accounted for.
        var total = 0;
        var leftTupleIterator = leftDatasetInstance.iterator();
        while (leftTupleIterator.hasNext()) {
            total += size(leftTupleIterator.next().getA());
        }
        return total;
    }

    @Override
    public BiIterator<A, B> iterator() {
        return new OriginalBiIterator<>(leftDatasetInstance, rightDatasetInstance, solutionView);
    }

    @Override
    public BiIterator<A, B> randomIterator(RandomGenerator random) {
        return new RandomBiIterator<>(leftDatasetInstance, rightDatasetInstance, solutionView, random);
    }

    @Override
    public int size(@Nullable A a) {
        return rightDatasetInstance.size(rightDatasetInstance.produceCompositeKey(a));
    }

    @Override
    public Iterator<@Nullable B> iterator(@Nullable A a) {
        var compositeKey = rightDatasetInstance.produceCompositeKey(a);
        Iterator<UniTuple<B>> tupleIterator = rightDatasetInstance.iterator(compositeKey);
        var filter = rightDatasetInstance.getFilter();
        if (filter != null) {
            tupleIterator = filterByLeft(tupleIterator, filter, solutionView, a);
        }
        return new FactIteratorAdapter<>(tupleIterator, false);
    }

    @Override
    public Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random) {
        var compositeKey = rightDatasetInstance.produceCompositeKey(a);
        var filter = rightDatasetInstance.getFilter();
        Iterator<UniTuple<B>> tupleIterator = filter == null
                ? rightDatasetInstance.randomIterator(compositeKey, random)
                : rightDatasetInstance.randomIterator(compositeKey, random,
                        rightTuple -> filter.test(solutionView, a, rightTuple.getA()));
        return new FactIteratorAdapter<>(tupleIterator, true);
    }

    private static <Solution_, A, B> Iterator<UniTuple<B>> filterByLeft(Iterator<UniTuple<B>> source,
            BiNeighborhoodsPredicate<Solution_, A, B> filter, SolutionView<Solution_> solutionView, @Nullable A leftFact) {
        return new Iterator<>() {
            private @Nullable UniTuple<B> next;

            @Override
            public boolean hasNext() {
                while (next == null && source.hasNext()) {
                    var candidate = source.next();
                    if (filter.test(solutionView, leftFact, candidate.getA())) {
                        next = candidate;
                    }
                }
                return next != null;
            }

            @Override
            public UniTuple<B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var result = Objects.requireNonNull(next);
                next = null;
                return result;
            }
        };
    }

    private static final class FactIteratorAdapter<B> implements Iterator<@Nullable B> {

        private final Iterator<UniTuple<B>> tupleIterator;
        private final boolean removeAfterNext;

        private FactIteratorAdapter(Iterator<UniTuple<B>> tupleIterator, boolean removeAfterNext) {
            this.tupleIterator = tupleIterator;
            this.removeAfterNext = removeAfterNext;
        }

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public @Nullable B next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = tupleIterator.next().getA();
            if (removeAfterNext) {
                tupleIterator.remove();
            }
            return result;
        }

    }

    /**
     * Ports {@code BiOriginalMoveIterator}'s left-then-right walk: fix a left tuple, walk all matching right
     * tuples, then advance to the next left tuple.
     */
    private static final class OriginalBiIterator<Solution_, A, B> implements BiIterator<A, B> {

        private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;
        private final SolutionView<Solution_> solutionView;
        private final @Nullable BiNeighborhoodsPredicate<Solution_, A, B> filter;
        private final Iterator<UniTuple<A>> leftTupleIterator;

        private @Nullable Iterator<UniTuple<B>> rightTupleIterator;
        private @Nullable UniTuple<A> leftTuple;
        private @Nullable UniTuple<A> pendingLeftTuple;
        private @Nullable UniTuple<B> pendingRightTuple;
        private @Nullable UniTuple<A> currentLeftTuple;
        private @Nullable UniTuple<B> currentRightTuple;

        private OriginalBiIterator(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance,
                UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance, SolutionView<Solution_> solutionView) {
            this.rightDatasetInstance = rightDatasetInstance;
            this.solutionView = solutionView;
            this.filter = rightDatasetInstance.getFilter();
            this.leftTupleIterator = leftDatasetInstance.iterator();
        }

        @Override
        public boolean hasNext() {
            if (pendingRightTuple != null) {
                return true;
            }
            while (true) {
                if (rightTupleIterator != null && rightTupleIterator.hasNext()) {
                    pendingLeftTuple = leftTuple;
                    pendingRightTuple = rightTupleIterator.next();
                    return true;
                }
                if (!leftTupleIterator.hasNext()) {
                    return false;
                }
                leftTuple = leftTupleIterator.next();
                var compositeKey = rightDatasetInstance.produceCompositeKey(leftTuple);
                Iterator<UniTuple<B>> raw = rightDatasetInstance.iterator(compositeKey);
                rightTupleIterator = filter == null ? raw : filterByLeft(raw, filter, solutionView, leftTuple.getA());
            }
        }

        @Override
        public void next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            currentLeftTuple = pendingLeftTuple;
            currentRightTuple = pendingRightTuple;
            pendingLeftTuple = null;
            pendingRightTuple = null;
        }

        @Override
        public @Nullable A getA() {
            return Objects.requireNonNull(currentLeftTuple).getA();
        }

        @Override
        public @Nullable B getB() {
            return Objects.requireNonNull(currentRightTuple).getA();
        }

    }

    /**
     * Ports {@code BiRandomMoveIterator}'s sampling-without-replacement walk over (A, B) pairs.
     */
    private static final class RandomBiIterator<Solution_, A, B> implements BiIterator<A, B> {

        private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;
        private final SolutionView<Solution_> solutionView;
        private final RandomGenerator workingRandom;
        private final Map<UniTuple<A>, Iterator<UniTuple<B>>> leftTupleToRightIteratorMap = new HashMap<>();
        private final Iterator<UniTuple<A>> leftTupleIterator;

        private @Nullable UniTuple<A> pendingLeftTuple;
        private @Nullable UniTuple<B> pendingRightTuple;
        private @Nullable UniTuple<A> currentLeftTuple;
        private @Nullable UniTuple<B> currentRightTuple;

        private RandomBiIterator(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance,
                UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance, SolutionView<Solution_> solutionView,
                RandomGenerator workingRandom) {
            this.rightDatasetInstance = rightDatasetInstance;
            this.solutionView = solutionView;
            this.workingRandom = workingRandom;
            this.leftTupleIterator = leftDatasetInstance.randomIterator(workingRandom);
        }

        @Override
        public boolean hasNext() {
            if (pendingRightTuple != null) {
                return true;
            }
            while (leftTupleIterator.hasNext()) {
                var leftTuple = leftTupleIterator.next();
                if (pickNext(leftTuple)) {
                    return true;
                }
                leftTupleIterator.remove();
                leftTupleToRightIteratorMap.remove(leftTuple);
            }
            return false;
        }

        private boolean pickNext(UniTuple<A> leftTuple) {
            var rightTupleIterator = leftTupleToRightIteratorMap.get(leftTuple);
            if (rightTupleIterator == null) {
                rightTupleIterator = createRightTupleIterator(leftTuple);
                if (!rightTupleIterator.hasNext()) {
                    return false;
                }
                leftTupleToRightIteratorMap.put(leftTuple, rightTupleIterator);
            } else if (!rightTupleIterator.hasNext()) {
                return false;
            }
            pendingLeftTuple = leftTuple;
            pendingRightTuple = rightTupleIterator.next();
            rightTupleIterator.remove();
            return true;
        }

        private Iterator<UniTuple<B>> createRightTupleIterator(UniTuple<A> leftTuple) {
            var compositeKey = rightDatasetInstance.produceCompositeKey(leftTuple);
            var filter = rightDatasetInstance.getFilter();
            if (filter == null) {
                return rightDatasetInstance.randomIterator(compositeKey, workingRandom);
            }
            return rightDatasetInstance.randomIterator(compositeKey, workingRandom,
                    rightTuple -> filter.test(solutionView, leftTuple.getA(), rightTuple.getA()));
        }

        @Override
        public void next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            currentLeftTuple = pendingLeftTuple;
            currentRightTuple = pendingRightTuple;
            pendingLeftTuple = null;
            pendingRightTuple = null;
        }

        @Override
        public @Nullable A getA() {
            return Objects.requireNonNull(currentLeftTuple).getA();
        }

        @Override
        public @Nullable B getB() {
            return Objects.requireNonNull(currentRightTuple).getA();
        }

    }

}
