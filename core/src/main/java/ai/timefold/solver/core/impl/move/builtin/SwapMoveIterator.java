package ai.timefold.solver.core.impl.move.builtin;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws the right entity only from the candidates of {@link MoveProviderUtil#swapCandidatesOf}.
 */
@NullMarked
public final class SwapMoveIterator<Solution_, Entity_> extends LeftDrivenMoveIterator<Solution_, Entity_> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final SolutionView<Solution_> solutionView;
    private final UniDatasetInstance<Entity_> entityInstance;
    private final RandomGenerator random;
    private @Nullable Iterator<@Nullable Entity_> anyEntityIterator;

    public SwapMoveIterator(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            SolutionView<Solution_> solutionView, UniDatasetInstance<Entity_> entityInstance, RandomGenerator random) {
        super(entityInstance.iterator(random), entityInstance.size());
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
