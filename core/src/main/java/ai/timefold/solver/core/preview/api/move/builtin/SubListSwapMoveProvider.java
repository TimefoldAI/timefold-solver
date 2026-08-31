package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.util.TriangleElementFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * For two contiguous spans ("sub-lists") of assigned runs of a list variable,
 * each governed by its own size bounds,
 * creates a move to swap them,
 * possibly on different entities,
 * possibly in reverse element order.
 * <p>
 * There is no {@code crossingNull} flag:
 * a swap of two spans cannot cross null by construction,
 * since both spans are drawn from assigned runs.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class SubListSwapMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final int leftMinimumSubListSize;
    private final int leftMaximumSubListSize;
    private final int rightMinimumSubListSize;
    private final int rightMaximumSubListSize;
    private final boolean selectReversingMoveToo;

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, 1, SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, 1,
                SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int minimumSubListSize, int maximumSubListSize) {
        this(variableMetaModel, minimumSubListSize, maximumSubListSize, minimumSubListSize, maximumSubListSize, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int leftMinimumSubListSize, int leftMaximumSubListSize, int rightMinimumSubListSize,
            int rightMaximumSubListSize) {
        this(variableMetaModel, leftMinimumSubListSize, leftMaximumSubListSize, rightMinimumSubListSize,
                rightMaximumSubListSize, true);
    }

    public SubListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            int leftMinimumSubListSize, int leftMaximumSubListSize, int rightMinimumSubListSize,
            int rightMaximumSubListSize, boolean selectReversingMoveToo) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        TriangleElementFactory.validateSizes(leftMinimumSubListSize, leftMaximumSubListSize);
        TriangleElementFactory.validateSizes(rightMinimumSubListSize, rightMaximumSubListSize);
        this.leftMinimumSubListSize = leftMinimumSubListSize;
        this.leftMaximumSubListSize = leftMaximumSubListSize;
        this.rightMinimumSubListSize = rightMinimumSubListSize;
        this.rightMaximumSubListSize = rightMaximumSubListSize;
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new SubListSwapMoveIterator<>(session, random,
                variableMetaModel, valueDataset, leftMinimumSubListSize, leftMaximumSubListSize,
                rightMinimumSubListSize, rightMaximumSubListSize, selectReversingMoveToo));
    }

    /**
     * Draws two spans sharing no particular relationship other than both being seeded by an assigned value,
     * producing a {@code SubListSwapMove}.
     * Left = seed value, right = candidate span.
     * <p>
     * A fresh left span is drawn on <strong>every</strong> {@link #createRightIterator} call,
     * and a fresh right span is drawn per candidate probed, never cached:
     * the same reasoning {@link SubPillarSwapMoveProvider} documents.
     * <p>
     * <strong>Known ceiling:</strong> see {@link SubListChangeMoveProvider}'s class javadoc for the same ceiling,
     * accepted for the same reason.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class SubListSwapMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, Value_> {

        private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final boolean selectReversingMoveToo;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> leftValueIterator;
        private final DefaultUniDatasetInstance<Solution_, Value_> valueInstance;
        private final SubListSampler<Solution_, Entity_, Value_> leftSampler;
        private final SubListSampler<Solution_, Entity_, Value_> rightSampler;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable Range<Entity_> pendingLeftRange = null;
        private @Nullable Range<Entity_> pendingRightRange = null;

        SubListSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> valueDataset, int leftMinimumSubListSize, int leftMaximumSubListSize,
                int rightMinimumSubListSize, int rightMaximumSubListSize, boolean selectReversingMoveToo) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.selectReversingMoveToo = selectReversingMoveToo;
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            this.valueInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset);
            this.leftValueIterator = valueInstance.retiringRandomIterator(random);
            this.leftSampler = Samplers.subList(variableMetaModel, leftMinimumSubListSize, leftMaximumSubListSize, random);
            this.rightSampler =
                    Samplers.subList(variableMetaModel, rightMinimumSubListSize, rightMaximumSubListSize, random);
        }

        @Override
        public boolean hasNext() {
            return nextMove != null || RetiringBiWalk.advance(leftValueIterator, this);
        }

        @Override
        public Move<Solution_> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var move = Objects.requireNonNull(nextMove);
            nextMove = null;
            return move;
        }

        @Override
        public Iterator<Value_> createRightIterator(Value_ leftValue) {
            // Fresh left span on every call; see the class javadoc.
            var leftRange = leftSampler.byValue(solutionView, leftValue);
            if (leftRange == null) {
                pendingLeftRange = null;
                return Collections.emptyIterator();
            }
            pendingLeftRange = leftRange;
            var bailOutSize = valueInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(valueInstance.iterator(random), candidateValue -> {
                var rightRange = rightSampler.byValue(solutionView, candidateValue);
                if (rightRange != null && isValidSwap(leftRange, rightRange)) {
                    pendingRightRange = rightRange;
                    return true;
                }
                return false;
            }, bailOutSize);
        }

        private boolean isValidSwap(Range<Entity_> left, Range<Entity_> right) {
            Entity_ leftEntity = left.entity();
            Entity_ rightEntity = right.entity();
            if (leftEntity == rightEntity) {
                // Must not overlap.
                return left.toIndex() <= right.fromIndex() || right.toIndex() <= left.fromIndex();
            }
            if (variableMetaModel.isValueRangeOnSolution()) {
                return true;
            }
            for (var index = left.fromIndex(); index < left.toIndex(); index++) {
                var value = solutionView.getValueAtIndex(variableMetaModel, leftEntity, index);
                if (!solutionView.isValueInRange(variableMetaModel, rightEntity, value)) {
                    return false;
                }
            }
            for (var index = right.fromIndex(); index < right.toIndex(); index++) {
                var value = solutionView.getValueAtIndex(variableMetaModel, rightEntity, index);
                if (!solutionView.isValueInRange(variableMetaModel, leftEntity, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void accept(Value_ leftValue, Value_ rightValue) {
            var left = Objects.requireNonNull(pendingLeftRange);
            var right = Objects.requireNonNull(pendingRightRange);
            var reversing = selectReversingMoveToo && (left.length() > 1 || right.length() > 1) && random.nextBoolean();
            nextMove = Moves.swap(variableMetaModel, left, right, reversing);
            pendingLeftRange = null;
            pendingRightRange = null;
        }

    }

}
