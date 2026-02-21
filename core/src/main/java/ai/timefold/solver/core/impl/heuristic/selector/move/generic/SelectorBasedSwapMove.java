package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedSwapMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    protected final List<GenuineVariableDescriptor<Solution_>> variableDescriptorList;

    protected final Object leftEntity;
    protected final Object rightEntity;

    public SelectorBasedSwapMove(List<GenuineVariableDescriptor<Solution_>> variableDescriptorList, Object leftEntity,
            Object rightEntity) {
        this.variableDescriptorList = variableDescriptorList;
        this.leftEntity = leftEntity;
        this.rightEntity = rightEntity;
    }

    public Object getLeftEntity() {
        return leftEntity;
    }

    public Object getRightEntity() {
        return rightEntity;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        if (leftEntity == rightEntity) {
            return false;
        }
        for (var variableDescriptor : variableDescriptorList) {
            var leftValue = variableDescriptor.getValue(leftEntity);
            var rightValue = variableDescriptor.getValue(rightEntity);
            if (!Objects.equals(leftValue, rightValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        for (var variableDescriptor : variableDescriptorList) {
            var oldLeftValue = variableDescriptor.getValue(leftEntity);
            var oldRightValue = variableDescriptor.getValue(rightEntity);
            if (!Objects.equals(oldLeftValue, oldRightValue)) {
                scoreDirector.changeVariableFacade(variableDescriptor, leftEntity, oldRightValue);
                scoreDirector.changeVariableFacade(variableDescriptor, rightEntity, oldLeftValue);
            }
        }
    }

    @Override
    public SelectorBasedSwapMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedSwapMove<>(variableDescriptorList, lookup.lookUpWorkingObject(leftEntity),
                lookup.lookUpWorkingObject(rightEntity));
    }

    @Override
    public String describe() {
        var moveTypeDescription = new StringBuilder(20 * (variableDescriptorList.size() + 1));
        moveTypeDescription.append("SwapMove(");
        var delimiter = "";
        for (var variableDescriptor : variableDescriptorList) {
            moveTypeDescription.append(delimiter).append(variableDescriptor.getSimpleEntityAndVariableName());
            delimiter = ", ";
        }
        moveTypeDescription.append(")");
        return moveTypeDescription.toString();
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(leftEntity, rightEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        var values = new ArrayList<>(variableDescriptorList.size() * 2);
        for (var variableDescriptor : variableDescriptorList) {
            values.add(variableDescriptor.getValue(leftEntity));
            values.add(variableDescriptor.getValue(rightEntity));
        }
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var swapMove = (SelectorBasedSwapMove<?>) o;
        return Objects.equals(variableDescriptorList, swapMove.variableDescriptorList) &&
                Objects.equals(leftEntity, swapMove.leftEntity) &&
                Objects.equals(rightEntity, swapMove.rightEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptorList, leftEntity, rightEntity);
    }

    @Override
    public String toString() {
        var s = new StringBuilder(variableDescriptorList.size() * 16);
        s.append(leftEntity).append(" {");
        appendVariablesToString(s, leftEntity);
        s.append("} <-> ");
        s.append(rightEntity).append(" {");
        appendVariablesToString(s, rightEntity);
        s.append("}");
        return s.toString();
    }

    protected void appendVariablesToString(StringBuilder s, Object entity) {
        var first = true;
        for (var variableDescriptor : variableDescriptorList) {
            if (!first) {
                s.append(", ");
            }
            var value = variableDescriptor.getValue(entity);
            s.append(value == null ? null : value.toString());
            first = false;
        }
    }

}
