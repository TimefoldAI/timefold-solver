package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniRightDatasetInstance;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiIterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BiDatasetInstance} produced by {@code UniDataset.join}:
 * the join is not materialized in Bavet but computed just in time inside this instance,
 * out of a left and a right {@code UniDataset}.
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
        var total = 0;
        for (var aUniTuple : leftDatasetInstance) {
            total += size(aUniTuple.getA());
        }
        return total;
    }

    @Override
    public BiIterator<A, B> iterator(RandomGenerator random) {
        return new RepeatingRandomBiIterator<>(leftDatasetInstance, rightDatasetInstance, solutionView, random);
    }

    @Override
    public BiIterator<A, B> exhaustiveIterator(RandomGenerator random) {
        return new UniqueRandomBiIterator<>(leftDatasetInstance, rightDatasetInstance, solutionView, random);
    }

    @Override
    public int size(@Nullable A a) {
        return rightDatasetInstance.size(rightDatasetInstance.produceCompositeKey(a));
    }

    @Override
    public Iterator<@Nullable B> iterator(@Nullable A a, RandomGenerator random) {
        var compositeKey = rightDatasetInstance.produceCompositeKey(a);
        var tupleIterator = rightDatasetInstance.randomIterator(compositeKey, random);
        var filter = rightDatasetInstance.getFilter();
        if (filter != null) {
            // Draws with replacement can never prove that no matching right tuple exists;
            // bail out after many consecutive rejections.
            var bailOutSize = rightDatasetInstance.size(compositeKey) * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            tupleIterator = new FilteringIterator<>(tupleIterator,
                    rightTuple -> filter.test(solutionView, a, rightTuple.getA()), bailOutSize);
        }
        return new FactIteratorAdapter<>(tupleIterator);
    }

    @Override
    public Iterator<@Nullable B> exhaustiveIterator(@Nullable A a, RandomGenerator random) {
        var compositeKey = rightDatasetInstance.produceCompositeKey(a);
        var filter = rightDatasetInstance.getFilter();
        var tupleIterator = filter == null
                ? rightDatasetInstance.uniqueRandomIterator(compositeKey, random)
                : rightDatasetInstance.uniqueRandomIterator(compositeKey, random,
                        rightTuple -> filter.test(solutionView, a, rightTuple.getA()));
        return new FactIteratorAdapter<>(tupleIterator);
    }

    /**
     * Maps a tuple iterator to its fact.
     * Uniqueness or endlessness (and any bail-out) are entirely a property of the wrapped {@code tupleIterator};
     * this class neither removes nor limits anything itself.
     */
    private record FactIteratorAdapter<B>(Iterator<UniTuple<B>> tupleIterator)
            implements
                Iterator<@Nullable B> {

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public @Nullable B next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return tupleIterator.next().getA();
        }

        @Override
        public void forEachRemaining(Consumer<? super @Nullable B> action) {
            tupleIterator.forEachRemaining(tuple -> action.accept(tuple.getA()));
        }

    }

    /**
     * Ports {@code BiRandomMoveIterator}'s never-ending walk over (A, B) pairs:
     * the right side is drawn with replacement,
     * and a left tuple is only ever retired once its right side is confirmed empty.
     */
    private static final class RepeatingRandomBiIterator<Solution_, A, B>
            implements BiIterator<A, B>, RetiringBiWalk<UniTuple<A>, UniTuple<B>> {

        private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;
        private final SolutionView<Solution_> solutionView;
        private final RandomGenerator workingRandom;
        private final RetiringRandomIterator<UniTuple<A>> leftTupleIterator;

        private @Nullable UniTuple<A> pendingLeftTuple;
        private @Nullable UniTuple<B> pendingRightTuple;
        private @Nullable UniTuple<A> currentLeftTuple;
        private @Nullable UniTuple<B> currentRightTuple;

        private RepeatingRandomBiIterator(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance,
                UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance, SolutionView<Solution_> solutionView,
                RandomGenerator workingRandom) {
            this.rightDatasetInstance = rightDatasetInstance;
            this.solutionView = solutionView;
            this.workingRandom = workingRandom;
            this.leftTupleIterator = leftDatasetInstance.retiringRandomIterator(workingRandom);
        }

        @Override
        public boolean hasNext() {
            return pendingRightTuple != null || RetiringBiWalk.advance(leftTupleIterator, this);
        }

        @Override
        public Iterator<UniTuple<B>> createRightIterator(UniTuple<A> leftTuple) {
            var compositeKey = rightDatasetInstance.produceCompositeKey(leftTuple);
            var rightTupleIterator = rightDatasetInstance.randomIterator(compositeKey, workingRandom);
            var filter = rightDatasetInstance.getFilter();
            if (filter == null) {
                return rightTupleIterator;
            }
            // RetiringBiWalk.advance() retries this call up to PROBE_ATTEMPT_COUNT times before retiring the
            // left, since a single bail-out is a false negative, not proof of emptiness.
            var bailOutSize = rightDatasetInstance.size(compositeKey) * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(rightTupleIterator,
                    rightTuple -> filter.test(solutionView, leftTuple.getA(), rightTuple.getA()), bailOutSize);
        }

        @Override
        public void accept(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
            pendingLeftTuple = leftTuple;
            pendingRightTuple = rightTuple;
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
        public @Nullable A a() {
            return currentLeftTuple.getA();
        }

        @Override
        public @Nullable B b() {
            return currentRightTuple.getA();
        }

    }

    /**
     * Ports {@code BiRandomMoveIterator}'s sampling-without-replacement walk over (A, B) pairs.
     */
    private static final class UniqueRandomBiIterator<Solution_, A, B>
            implements BiIterator<A, B>, RetiringBiWalk<UniTuple<A>, UniTuple<B>> {

        private final UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance;
        private final SolutionView<Solution_> solutionView;
        private final RandomGenerator workingRandom;
        private final Map<UniTuple<A>, Iterator<UniTuple<B>>> leftTupleToRightIteratorMap = new HashMap<>();
        private final RetiringRandomIterator<UniTuple<A>> leftTupleIterator;

        private @Nullable UniTuple<A> pendingLeftTuple;
        private @Nullable UniTuple<B> pendingRightTuple;
        private @Nullable UniTuple<A> currentLeftTuple;
        private @Nullable UniTuple<B> currentRightTuple;

        private UniqueRandomBiIterator(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> leftDatasetInstance,
                UniRightDatasetInstance<Solution_, A, B> rightDatasetInstance, SolutionView<Solution_> solutionView,
                RandomGenerator workingRandom) {
            this.rightDatasetInstance = rightDatasetInstance;
            this.solutionView = solutionView;
            this.workingRandom = workingRandom;
            this.leftTupleIterator = leftDatasetInstance.retiringRandomIterator(workingRandom);
        }

        @Override
        public boolean hasNext() {
            return pendingRightTuple != null || RetiringBiWalk.advance(leftTupleIterator, this);
        }

        @Override
        public Iterator<UniTuple<B>> createRightIterator(UniTuple<A> leftTuple) {
            var rightTupleIterator = leftTupleToRightIteratorMap.get(leftTuple);
            if (rightTupleIterator == null) {
                rightTupleIterator = createRightTupleIterator(leftTuple);
                leftTupleToRightIteratorMap.put(leftTuple, rightTupleIterator);
            }
            return rightTupleIterator;
        }

        private Iterator<UniTuple<B>> createRightTupleIterator(UniTuple<A> leftTuple) {
            var compositeKey = rightDatasetInstance.produceCompositeKey(leftTuple);
            var rightTupleIterator = rightDatasetInstance.uniqueRandomIterator(compositeKey, workingRandom);
            var filter = rightDatasetInstance.getFilter();
            if (filter == null) {
                return rightTupleIterator;
            }
            // The delegate already guarantees uniqueness; nothing to bail out of.
            return new FilteringIterator<>(rightTupleIterator,
                    rightTuple -> filter.test(solutionView, leftTuple.getA(), rightTuple.getA()));
        }

        @Override
        public void accept(UniTuple<A> leftTuple, UniTuple<B> rightTuple) {
            pendingLeftTuple = leftTuple;
            pendingRightTuple = rightTuple;
        }

        @Override
        public void onExhausted(UniTuple<A> leftTuple) {
            leftTupleToRightIteratorMap.remove(leftTuple);
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
        public @Nullable A a() {
            return currentLeftTuple.getA();
        }

        @Override
        public @Nullable B b() {
            return currentRightTuple.getA();
        }

    }

}
