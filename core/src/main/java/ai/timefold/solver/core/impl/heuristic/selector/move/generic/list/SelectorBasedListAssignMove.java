package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class SelectorBasedListAssignMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object planningValue;
    private final Object destinationEntity;
    private final int destinationIndex;

    public SelectorBasedListAssignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object planningValue,
            Object destinationEntity,
            int destinationIndex) {
        this.variableDescriptor = variableDescriptor;
        this.planningValue = planningValue;
        this.destinationEntity = destinationEntity;
        this.destinationIndex = destinationIndex;
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
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(destinationEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.of(planningValue);
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return destinationIndex >= 0 && variableDescriptor.getListSize(destinationEntity) >= destinationIndex;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        // Add planningValue to destinationEntity's list variable (at destinationIndex).
        scoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex + 1);
        scoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
    }

    @Override
    public SelectorBasedListAssignMove<Solution_> rebase(Rebaser rebaser) {
        return new SelectorBasedListAssignMove<>(variableDescriptor, Objects.requireNonNull(rebaser.rebase(planningValue)),
                Objects.requireNonNull(rebaser.rebase(destinationEntity)), destinationIndex);
    }

    @Override
    public String describe() {
        return "ListAssignMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedListAssignMove<?>) o;
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
