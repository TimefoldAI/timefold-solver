package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractEasyMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubListUnassignMove<Solution_> extends AbstractEasyMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final int length;

    private Collection<Object> planningValues;

    public SubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, SubList subList) {
        this(variableDescriptor, subList.entity(), subList.fromIndex(), subList.length());
    }

    public SubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity, int sourceIndex,
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
        return true;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;

        var sourceList = variableDescriptor.getValue(sourceEntity);
        var subList = sourceList.subList(sourceIndex, sourceIndex + length);
        planningValues = new ArrayList<>(subList);

        innerScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + length);
        subList.clear();
        innerScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        for (var element : planningValues) {
            innerScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
            innerScoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
        }
    }

    @Override
    public SubListUnassignMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubListUnassignMove<>(variableDescriptor, destinationScoreDirector.lookUpWorkingObject(sourceEntity),
                sourceIndex, length);
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        var entities = new LinkedHashSet<>(2);
        entities.add(sourceEntity);
        return entities;
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
