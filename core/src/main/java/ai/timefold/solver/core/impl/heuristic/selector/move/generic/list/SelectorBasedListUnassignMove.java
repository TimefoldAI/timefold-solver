package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SelectorBasedListUnassignMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;

    private @Nullable Object movedValue;

    public SelectorBasedListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity,
            int sourceIndex) {
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

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(sourceEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.of(getMovedValue());
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return variableDescriptor.getElement(sourceEntity, sourceIndex) != null;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        var listVariable = variableDescriptor.getValue(sourceEntity);
        var element = getMovedValue();
        // Remove an element from sourceEntity's list variable (at sourceIndex).
        scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
        scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        movedValue = listVariable.remove(sourceIndex);
        scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        scoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
    }

    @Override
    public SelectorBasedListUnassignMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedListUnassignMove<>(variableDescriptor, lookup.lookUpWorkingObject(sourceEntity), sourceIndex);
    }

    @Override
    public String describe() {
        return "ListUnassignMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SelectorBasedListUnassignMove<?> other) {
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
