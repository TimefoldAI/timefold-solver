package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveIteratorSession;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
public final class ListTailSwapMoveIterator<Solution_, Entity_, Value_>
        extends AbstractRepeatedDrawMoveIterator<Solution_, Value_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean selectReversingMoveToo;
    private final RandomGenerator random;
    private final SolutionView<Solution_> solutionView;
    private final DefaultUniDatasetInstance<Solution_, Value_> valueInstance;
    // Only with a solution value range.
    private final @Nullable Iterator<Value_> rightValueIterator;

    public ListTailSwapMoveIterator(MoveIteratorSession<Solution_> session, RandomGenerator random,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            DefaultUniDatasetInstance<Solution_, Value_> valueInstance, boolean selectReversingMoveToo) {
        super(valueInstance.retiringRandomIterator(random));
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.selectReversingMoveToo = selectReversingMoveToo;
        this.random = Objects.requireNonNull(random);
        this.solutionView = session.getSolutionView();
        this.valueInstance = valueInstance;
        this.rightValueIterator = variableMetaModel.isValueRangeOnSolution() ? valueInstance.iterator(random) : null;
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Value_ leftValue) {
        var leftPosition = (PositionInList) solutionView.getPositionOf(variableMetaModel, leftValue);
        Value_ rightValue;
        if (variableMetaModel.isValueRangeOnSolution()) {
            // No per-entity value range to violate; a same-entity tail swap is degenerate, as the tails would overlap.
            rightValue = drawMatching(Objects.requireNonNull(rightValueIterator), valueInstance.size(),
                    candidateValue -> ((PositionInList) solutionView
                            .getPositionOf(variableMetaModel, candidateValue)).entity() != leftPosition.entity());
        } else {
            rightValue = drawValidPartner(leftPosition);
        }
        return rightValue == null ? null : buildMove(leftPosition, rightValue);
    }

    /**
     * With entity value ranges, the valid partners are found directly, so that a left value with none fails quickly.
     * A partner entity must accept the whole left tail,
     * and in it, the valid tail starts are the indices from which every value fits the left entity.
     * Every valid partner has the same chance.
     *
     * @return null if the left value has no valid partner
     */
    private @Nullable Value_ drawValidPartner(PositionInList leftPosition) {
        Entity_ leftEntity = leftPosition.entity();
        var leftSize = solutionView.countValues(variableMetaModel, leftEntity);
        var leftTail = new ArrayList<Value_>(leftSize - leftPosition.index());
        for (var index = leftPosition.index(); index < leftSize; index++) {
            leftTail.add(solutionView.getValueAtIndex(variableMetaModel, leftEntity, index));
        }
        var narrowestList = solutionView.getEntitiesWithValueInRange(variableMetaModel,
                ListCandidatePool.narrowestValue(solutionView, variableMetaModel, leftTail));
        var partnerEntityList = new ArrayList<Entity_>();
        var firstStartList = new ArrayList<Integer>();
        var partnerCount = 0;
        var leftRange = solutionView.getValueRange(variableMetaModel, leftEntity);
        for (var entity : narrowestList) {
            if (entity == leftEntity || !MoveProviderUtil.acceptsAll(solutionView, variableMetaModel, entity, leftTail)) {
                continue;
            }
            var size = solutionView.countValues(variableMetaModel, entity);
            var firstStart = size;
            var firstUnpinnedIndex = solutionView.getFirstUnpinnedIndex(variableMetaModel, entity);
            while (firstStart > firstUnpinnedIndex
                    && leftRange.contains(solutionView.getValueAtIndex(variableMetaModel, entity, firstStart - 1))) {
                firstStart--;
            }
            if (firstStart < size) {
                partnerEntityList.add(entity);
                firstStartList.add(firstStart);
                partnerCount += size - firstStart;
            }
        }
        if (partnerCount == 0) {
            return null;
        }
        var pick = random.nextInt(partnerCount);
        for (var i = 0; i < partnerEntityList.size(); i++) {
            var entity = partnerEntityList.get(i);
            var firstStart = firstStartList.get(i);
            var count = solutionView.countValues(variableMetaModel, entity) - firstStart;
            if (pick < count) {
                return solutionView.getValueAtIndex(variableMetaModel, entity, firstStart + pick);
            }
            pick -= count;
        }
        throw new IllegalStateException("Impossible state: the pick (%d) is outside of the partner count (%d)."
                .formatted(pick, partnerCount));
    }

    private Move<Solution_> buildMove(PositionInList leftPosition, Value_ rightValue) {
        var rightPosition = (PositionInList) solutionView.getPositionOf(variableMetaModel, rightValue);
        Entity_ leftEntity = leftPosition.entity();
        Entity_ rightEntity = rightPosition.entity();
        var leftSize = solutionView.countValues(variableMetaModel, leftEntity);
        var rightSize = solutionView.countValues(variableMetaModel, rightEntity);
        var reversing =
                selectReversingMoveToo && (leftSize - leftPosition.index() > 1 || rightSize - rightPosition.index() > 1)
                        && random.nextBoolean();
        return Moves.swap(variableMetaModel, new Range<>(leftEntity, leftPosition.index(), leftSize),
                new Range<>(rightEntity, rightPosition.index(), rightSize), reversing);
    }

}
