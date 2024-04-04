package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSimplifiedMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubListUnassignMove<Solution_> extends AbstractSimplifiedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final int length;

    private Collection<Object> planningValues;

    public SubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, SubList subList) {
        this(variableDescriptor, subList.entity(), subList.fromIndex(), subList.length());
    }

    private SubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity, int sourceIndex,
            int length) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
        this.length = length;
    }

    public Object getSourceEntity() {
        return sourceEntity;
    }

    public int getFromIndex() {
        return sourceIndex;
    }

    public int getSubListSize() {
        return length;
    }

    public int getToIndex() {
        return sourceIndex + length;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        if (sourceIndex < 0) {
            return false;
        }
        return variableDescriptor.getListSize(sourceEntity) >= getToIndex();
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;

        var sourceList = variableDescriptor.getValue(sourceEntity);
        var subList = sourceList.subList(sourceIndex, getToIndex());
        planningValues = List.copyOf(subList);

        for (var element : subList) {
            innerScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
            innerScoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
        }
        innerScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, getToIndex());
        subList.clear();
        innerScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
    }

    @Override
    public SubListUnassignMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubListUnassignMove<>(variableDescriptor, destinationScoreDirector.lookUpWorkingObject(sourceEntity),
                sourceIndex, length);
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<Object> getPlanningEntities() {
        return List.of(sourceEntity);
    }

    @Override
    public Collection<Object> getPlanningValues() {
        return planningValues;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SubListUnassignMove<?> other) {
            return sourceIndex == other.sourceIndex && length == other.length
                    && variableDescriptor.equals(other.variableDescriptor)
                    && sourceEntity.equals(other.sourceEntity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex, length);
    }

    @Override
    public String toString() {
        return String.format("|%d| {%s[%d..%d] -> null}",
                length, sourceEntity, sourceIndex, getToIndex());
    }
}
