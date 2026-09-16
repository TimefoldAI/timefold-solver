package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws two assigned values on different entities and creates a move to swap their tails -
 * the portion of each entity's {@link PlanningListVariable list variable} from the drawn position to the end -
 * optionally in reverse element order.
 * <p>
 * This is the cross-entity half of the classic 2-opt route-improving move, at full rate.
 * {@link TwoOptListMoveProvider} makes this same move too,
 * whenever its own {@code crossingEntity} is {@code true} -
 * but there, only non-reversing, and only as one destination row among many,
 * so it arrives rarely.
 * This class exists to make it happen often, and to also emit the reversing variant.
 * <p>
 * The swapped tails are unbounded by design:
 * a tail swap is defined by the tails,
 * and bounding either one would produce a sub-list relocation instead,
 * which {@code SubListChangeMoveProvider} already provides with a size cap.
 *
 * @see TwoOptListMoveProvider The same-entity reversal, and this same shape at a much lower rate.
 * @see SubListChangeMoveProvider A bounded-size relocation instead of a whole tail.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class ListTailSwapMoveProvider<Solution_, Entity_, Value_> implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean selectReversingMoveToo;

    public ListTailSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, true);
    }

    /**
     * @param selectReversingMoveToo if {@code true}, also emits the reversing tail swap variant
     */
    public ListTailSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean selectReversingMoveToo) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueDataset = moveStreamFactory.forEachAssignedValue(variableMetaModel).asCachedDataset();
        return moveStreamFactory.buildMoveStream((session, random) -> new ListTailSwapMoveIterator<>(session, random,
                variableMetaModel, valueDataset, selectReversingMoveToo));
    }

    /**
     * Draws two assigned values on different entities and swaps their tails,
     * producing a {@code SubListSwapMove}.
     * Left = seed value, right = candidate value.
     *
     * @param <Solution_> the solution type
     * @param <Entity_> the entity type
     * @param <Value_> the variable type
     */
    @NullMarked
    private static final class ListTailSwapMoveIterator<Solution_, Entity_, Value_>
            implements Iterator<Move<Solution_>>, RetiringBiWalk<Value_, Value_> {

        private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
        private final boolean selectReversingMoveToo;
        private final RandomGenerator random;
        private final SolutionView<Solution_> solutionView;
        private final RetiringRandomIterator<Value_> leftValueIterator;
        private final DefaultUniDatasetInstance<Solution_, Value_> valueInstance;

        private @Nullable Move<Solution_> nextMove = null;
        private @Nullable PositionInList pendingLeftPosition = null;

        ListTailSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                UniDataset<Solution_, Value_> valueDataset, boolean selectReversingMoveToo) {
            this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
            this.selectReversingMoveToo = selectReversingMoveToo;
            this.random = Objects.requireNonNull(random);
            this.solutionView = session.getSolutionView();
            this.valueInstance = (DefaultUniDatasetInstance<Solution_, Value_>) session.getInstance(valueDataset);
            this.leftValueIterator = valueInstance.retiringRandomIterator(random);
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
            var leftPosition = (PositionInList) solutionView.getPositionOf(variableMetaModel, leftValue);
            pendingLeftPosition = leftPosition;
            var bailOutSize = valueInstance.size() * FilteringIterator.BAIL_OUT_SAFETY_MULTIPLIER;
            return new FilteringIterator<>(valueInstance.iterator(random),
                    candidateValue -> isValidTailSwap(leftPosition,
                            (PositionInList) solutionView.getPositionOf(variableMetaModel, candidateValue)),
                    bailOutSize);
        }

        private boolean isValidTailSwap(PositionInList left, PositionInList right) {
            Entity_ leftEntity = left.entity();
            Entity_ rightEntity = right.entity();
            if (leftEntity == rightEntity) {
                // A same-entity "tail swap" is degenerate: the two tails would necessarily overlap.
                return false;
            }
            if (variableMetaModel.isValueRangeOnSolution()) {
                // We can move freely between entities, no per-entity value range to violate.
                return true;
            }
            for (var index = left.index(); index < solutionView.countValues(variableMetaModel, leftEntity); index++) {
                var value = solutionView.getValueAtIndex(variableMetaModel, leftEntity, index);
                if (!solutionView.isValueInRange(variableMetaModel, rightEntity, value)) {
                    return false;
                }
            }
            for (var index = right.index(); index < solutionView.countValues(variableMetaModel, rightEntity); index++) {
                var value = solutionView.getValueAtIndex(variableMetaModel, rightEntity, index);
                if (!solutionView.isValueInRange(variableMetaModel, leftEntity, value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void accept(Value_ leftValue, Value_ rightValue) {
            var leftPosition = Objects.requireNonNull(pendingLeftPosition);
            pendingLeftPosition = null;
            var rightPosition = (PositionInList) solutionView.getPositionOf(variableMetaModel, rightValue);
            Entity_ leftEntity = leftPosition.entity();
            Entity_ rightEntity = rightPosition.entity();
            var leftSize = solutionView.countValues(variableMetaModel, leftEntity);
            var rightSize = solutionView.countValues(variableMetaModel, rightEntity);
            var reversing =
                    selectReversingMoveToo && (leftSize - leftPosition.index() > 1 || rightSize - rightPosition.index() > 1)
                            && random.nextBoolean();
            nextMove = Moves.swap(variableMetaModel, new Range<>(leftEntity, leftPosition.index(), leftSize),
                    new Range<>(rightEntity, rightPosition.index(), rightSize), reversing);
        }

    }

}
