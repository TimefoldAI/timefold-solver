package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractEasyMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public final class ListInitializeMove<Solution_> extends AbstractEasyMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object planningValue;

    public ListInitializeMove(ListVariableDescriptor<Solution_> variableDescriptor, Object planningValue) {
        this.variableDescriptor = variableDescriptor;
        this.planningValue = planningValue;
    }

    public Object getInitializedValue() {
        return planningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var variableDescriptorAwareScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        variableDescriptorAwareScoreDirector.beforeListVariableElementInitialized(variableDescriptor, planningValue);
        variableDescriptorAwareScoreDirector.afterListVariableElementInitialized(variableDescriptor, planningValue);
    }

    @Override
    public ListInitializeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new ListInitializeMove<>(variableDescriptor, destinationScoreDirector.lookUpWorkingObject(planningValue));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

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
        ListInitializeMove<?> other = (ListInitializeMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor)
                && Objects.equals(planningValue, other.planningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, planningValue);
    }

    @Override
    public String toString() {
        return String.format("Initialize(%s)", getInitializedValue());
    }

}
