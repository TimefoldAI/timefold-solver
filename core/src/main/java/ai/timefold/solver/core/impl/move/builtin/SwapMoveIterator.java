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
 * {@code isValidSwap} still checks both sides, because the list guarantees only one variable.
 */
@NullMarked
public final class SwapMoveIterator<Solution_, Entity_> extends AbstractLeftDrivenMoveIterator<Solution_, Entity_> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final SolutionView<Solution_> solutionView;
    private final DefaultUniDatasetInstance<Solution_, Entity_> entityInstance;
    private final RandomGenerator random;
    private @Nullable Iterator<@Nullable Entity_> anyEntityIterator;

    public SwapMoveIterator(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            SolutionView<Solution_> solutionView, DefaultUniDatasetInstance<Solution_, Entity_> entityInstance,
            RandomGenerator random) {
        super(entityInstance.retiringRandomIterator(random));
        this.variableMetaModelList = variableMetaModelList;
        this.solutionView = solutionView;
        this.entityInstance = entityInstance;
        this.random = random;
    }

    @Override
    protected @Nullable Move<Solution_> tryBuildMove(Entity_ leftEntity) {
        var candidateList = MoveProviderUtil.swapCandidatesOf(solutionView, variableMetaModelList, leftEntity);
        Entity_ rightEntity;
        if (candidateList == null) { // Every variable is unassigned; no value narrows the candidates.
            if (anyEntityIterator == null) {
                anyEntityIterator = entityInstance.iterator(random);
            }
            rightEntity = drawMatching(anyEntityIterator, entityInstance.size(),
                    candidate -> candidate != null && isValidSwap(solutionView, leftEntity, candidate));
        } else {
            rightEntity = drawMatching(candidateList, random,
                    candidate -> isValidSwap(solutionView, leftEntity, candidate));
        }
        return rightEntity == null ? null : Moves.swap(variableMetaModelList, leftEntity, rightEntity);
    }

    private boolean isValidSwap(SolutionView<Solution_> solutionView, Entity_ leftEntity, Entity_ rightEntity) {
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
            // Left side first: the candidate list already guarantees the right side for one variable.
            if (solutionView.isValueInRange(variableMetaModel, leftEntity, oldRightValue)
                    && solutionView.isValueInRange(variableMetaModel, rightEntity, oldLeftValue)) {
                change = true;
            } else {
                // One of the swaps falls out of range, skip this pair altogether.
                return false;
            }
        }
        return change;
    }

}
