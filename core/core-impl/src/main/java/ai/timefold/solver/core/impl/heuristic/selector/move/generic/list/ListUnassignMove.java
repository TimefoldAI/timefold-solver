package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSimplifiedMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public class ListUnassignMove<Solution_> extends AbstractSimplifiedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private Object movedValue;

    public ListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity, int sourceIndex) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
    }

    public Object getSourceEntity() {
        return sourceEntity;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public Object getMovedValue() {
        if (movedValue == null) {
            movedValue = variableDescriptor.getElement(sourceEntity, sourceIndex);
        }
        return movedValue;
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
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        var listVariable = variableDescriptor.getValue(sourceEntity);
        Object element = listVariable.get(sourceIndex);
        // Remove an element from sourceEntity's list variable (at sourceIndex).
        innerScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
        innerScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        movedValue = listVariable.remove(sourceIndex);
        innerScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        innerScoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
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
        if (o instanceof ListUnassignMove<?> other) {
            return sourceIndex == other.sourceIndex
                    && Objects.equals(variableDescriptor, other.variableDescriptor)
                    && Objects.equals(sourceEntity, other.sourceEntity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d] -> null}",
                getMovedValue(), sourceEntity, sourceIndex);
    }
}
