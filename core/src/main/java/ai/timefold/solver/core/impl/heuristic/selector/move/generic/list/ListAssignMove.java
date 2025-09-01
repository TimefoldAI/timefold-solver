package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class ListAssignMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object planningValue;
    private final Object destinationEntity;
    private final int destinationIndex;
    private final boolean checkValueRange;

    public ListAssignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object planningValue, Object destinationEntity,
            int destinationIndex) {
        // We don't enable the value range validation by default
        this(variableDescriptor, planningValue, destinationEntity, destinationIndex, false);
    }

    public ListAssignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object planningValue, Object destinationEntity,
            int destinationIndex, boolean checkValueRange) {
        this.variableDescriptor = variableDescriptor;
        this.planningValue = planningValue;
        this.destinationEntity = destinationEntity;
        this.destinationIndex = destinationIndex;
        this.checkValueRange = checkValueRange;
    }

    public Object getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    public Object getMovedValue() {
        return planningValue;
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return List.of(destinationEntity);
    }

    @Override
    public Collection<?> getPlanningValues() {
        return List.of(planningValue);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        var doable = destinationIndex >= 0 && variableDescriptor.getListSize(destinationEntity) >= destinationIndex;
        if (!doable || variableDescriptor.canExtractValueRangeFromSolution() || !checkValueRange) {
            return doable;
        }
        // When the value range is located at the entity,
        // we need to check if the destination's value range accepts the upcoming value
        ValueRangeManager<Solution_> valueRangeManager =
                ((VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector).getValueRangeManager();
        return valueRangeManager
                .getFromEntity(variableDescriptor.getValueRangeDescriptor(), destinationEntity)
                .contains(planningValue);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var variableDescriptorAwareScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        // Add planningValue to destinationEntity's list variable (at destinationIndex).
        variableDescriptorAwareScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        variableDescriptorAwareScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        variableDescriptorAwareScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        variableDescriptorAwareScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
    }

    @Override
    public ListAssignMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new ListAssignMove<>(variableDescriptor, destinationScoreDirector.lookUpWorkingObject(planningValue),
                destinationScoreDirector.lookUpWorkingObject(destinationEntity), destinationIndex);
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListAssignMove<?> other = (ListAssignMove<?>) o;
        return destinationIndex == other.destinationIndex
                && Objects.equals(variableDescriptor, other.variableDescriptor)
                && Objects.equals(planningValue, other.planningValue)
                && Objects.equals(destinationEntity, other.destinationEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, planningValue, destinationEntity, destinationIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {null -> %s[%d]}", getMovedValue(), destinationEntity, destinationIndex);
    }
}
