package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

/**
 * For each entity's {@link PlanningListVariable list variable},
 * draws two assigned positions.
 * When both are on the same entity, reverses the closed span between them in place -
 * the classic 2-opt route-improving move.
 * <p>
 * When {@code crossingEntity} is {@code true},
 * also allows a non-reversing tail swap between two different entities,
 * as one destination candidate among many - so it arrives rarely.
 * <p>
 * The reversed span is unbounded by design:
 * for a symmetric cost function a reversal changes only the two edges at its ends,
 * so its score impact is independent of the span's length,
 * unlike the {@code O(span)} notification cost of executing it.
 * Capping the span would remove the classic 2-opt neighborhood for no scoring reason.
 *
 * @see ListTailSwapMoveProvider The dedicated, full-rate counterpart for the cross-entity shape.
 * @see SubListChangeMoveProvider A bounded-size reversing relocation instead of an unbounded 2-opt.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the variable type
 */
@NullMarked
public final class TwoOptListMoveProvider<Solution_, Entity_, Value_> implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final boolean crossingEntity;

    public TwoOptListMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this(variableMetaModel, true);
    }

    /**
     * @param crossingEntity if {@code true}, also allows a non-reversing tail swap between two different entities,
     *        as one destination candidate among many;
     *        see {@link ListTailSwapMoveProvider} for a dedicated, full-rate counterpart
     */
    public TwoOptListMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            boolean crossingEntity) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.crossingEntity = crossingEntity;
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var positionStream = moveStreamFactory.forEachAssignedValue(variableMetaModel)
                .map((solutionView, value) -> (PositionInList) solutionView.getPositionOf(variableMetaModel, value));
        return moveStreamFactory.pick(positionStream)
                .pick(positionStream, NeighborhoodsJoiners.filtering(this::isValidTwoOpt))
                .asMove(this::buildMove);
    }

    private boolean isValidTwoOpt(SolutionView<Solution_> solutionView, PositionInList left, PositionInList right) {
        if (Objects.equals(left, right)) {
            return false;
        }
        Entity_ leftEntity = left.entity();
        Entity_ rightEntity = right.entity();
        if (leftEntity == rightEntity) {
            return true;
        }
        if (!crossingEntity) {
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

    private Move<Solution_> buildMove(SolutionView<Solution_> solutionView, PositionInList left, PositionInList right) {
        Entity_ leftEntity = left.entity();
        Entity_ rightEntity = right.entity();
        if (leftEntity == rightEntity) {
            var fromIndex = Math.min(left.index(), right.index());
            var toIndex = Math.max(left.index(), right.index()) + 1;
            return Moves.reverse(variableMetaModel, new Range<>(leftEntity, fromIndex, toIndex));
        }
        var leftSize = solutionView.countValues(variableMetaModel, leftEntity);
        var rightSize = solutionView.countValues(variableMetaModel, rightEntity);
        return Moves.swap(variableMetaModel, new Range<>(leftEntity, left.index(), leftSize),
                new Range<>(rightEntity, right.index(), rightSize), false);
    }

}
