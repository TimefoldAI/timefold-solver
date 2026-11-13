package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws the right entity only from the candidates of {@link MoveProviderUtil#swapCandidatesOf}.
 * Each candidate already accepts the left entity's value of one variable,
 * so a draw fails only if the left entity rejects the candidate's value,
 * instead of if either side rejects the other's; with narrow value ranges, far fewer draws fail.
 * With one variable, {@code isValidSwap} therefore checks only the left side;
 * with several, it checks both sides, because the list guarantees only one variable.
 */
@NullMarked
public final class SwapMoveIterator<Solution_, Entity_> extends AbstractRepeatedDrawMoveIterator<Solution_, Entity_> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final SolutionView<Solution_> solutionView;
    private final DefaultUniDatasetInstance<Solution_, Entity_> entityInstance;
    private final RandomGenerator random;
    // With only solution value ranges, every movable entity is a candidate; looking them up narrows nothing.
    private final boolean candidatesMayNarrow;
    private @Nullable Iterator<@Nullable Entity_> anyEntityIterator;

    public SwapMoveIterator(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            SolutionView<Solution_> solutionView, DefaultUniDatasetInstance<Solution_, Entity_> entityInstance,
            RandomGenerator random) {
        super(entityInstance.retiringRandomIterator(random));
        this.variableMetaModelList = variableMetaModelList;
        this.solutionView = solutionView;
        this.entityInstance = entityInstance;
        this.random = random;
        this.candidatesMayNarrow = variableMetaModelList.stream().anyMatch(v -> !v.isValueRangeOnSolution());
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Entity_ leftEntity) {
        var candidateList =
                candidatesMayNarrow ? MoveProviderUtil.swapCandidatesOf(solutionView, variableMetaModelList, leftEntity) : null;
        Entity_ rightEntity;
        if (candidateList == null) { // No value narrows the candidates.
            if (anyEntityIterator == null) {
                anyEntityIterator = entityInstance.iterator(random);
            }
            rightEntity = drawMatching(anyEntityIterator, entityInstance.size(),
                    candidate -> candidate != null && isValidSwap(solutionView, leftEntity, candidate, false));
        } else {
            // With one variable, every candidate accepts the left value; only the left side is checked.
            var rightAcceptsLeft = variableMetaModelList.size() == 1;
            rightEntity = drawMatching(candidateList, random,
                    candidate -> isValidSwap(solutionView, leftEntity, candidate, rightAcceptsLeft));
        }
        return rightEntity == null ? null : Moves.swap(variableMetaModelList, leftEntity, rightEntity);
    }

    /**
     * @param rightAcceptsLeft true if the right entity is known to accept the left entity's value of the only variable
     */
    private boolean isValidSwap(SolutionView<Solution_> solutionView, Entity_ leftEntity, Entity_ rightEntity,
            boolean rightAcceptsLeft) {
        if (leftEntity == rightEntity) {
            return false;
        }
        var change = false;
        for (var variableMetaModel : variableMetaModelList) {
            var defaultVariableMetaModel = (DefaultPlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel;
            var variableDescriptor = defaultVariableMetaModel.variableDescriptor();
            var oldLeftValue = variableDescriptor.getValue(leftEntity);
            var oldRightValue = variableDescriptor.getValue(rightEntity);
            if (Objects.equals(oldLeftValue, oldRightValue)) {
                continue;
            }
            // A solution value range accepts every value, except null if the variable does not allow it.
            if ((variableMetaModel.isValueRangeOnSolution() && oldLeftValue != null && oldRightValue != null)
                    || (solutionView.isValueInRange(variableMetaModel, leftEntity, oldRightValue)
                            && (rightAcceptsLeft
                                    || solutionView.isValueInRange(variableMetaModel, rightEntity, oldLeftValue)))) {
                change = true;
            } else {
                // One of the swaps falls out of range, skip this pair altogether.
                return false;
            }
        }
        return change;
    }

}
