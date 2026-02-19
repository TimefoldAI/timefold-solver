package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Move} is not cacheable.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedPillarSwapMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    protected final List<GenuineVariableDescriptor<Solution_>> variableDescriptorList;

    protected final List<Object> leftPillar;
    protected final List<Object> rightPillar;

    public SelectorBasedPillarSwapMove(List<GenuineVariableDescriptor<Solution_>> variableDescriptorList,
            List<Object> leftPillar, List<Object> rightPillar) {
        this.variableDescriptorList = variableDescriptorList;
        this.leftPillar = leftPillar;
        this.rightPillar = rightPillar;
    }

    public List<String> getVariableNameList() {
        List<String> variableNameList = new ArrayList<>(variableDescriptorList.size());
        for (var variableDescriptor : variableDescriptorList) {
            variableNameList.add(variableDescriptor.getVariableName());
        }
        return variableNameList;
    }

    public List<Object> getLeftPillar() {
        return leftPillar;
    }

    public List<Object> getRightPillar() {
        return rightPillar;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        var movable = false;
        for (var variableDescriptor : variableDescriptorList) {
            var leftValue = variableDescriptor.getValue(leftPillar.getFirst());
            var rightValue = variableDescriptor.getValue(rightPillar.getFirst());
            if (!Objects.equals(leftValue, rightValue)) {
                movable = true;
                if (!variableDescriptor.canExtractValueRangeFromSolution()) {
                    var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
                    for (var rightEntity : rightPillar) {
                        var rightValueRange =
                                extractValueRangeFromEntity(scoreDirector, valueRangeDescriptor, rightEntity);
                        if (!rightValueRange.contains(leftValue)) {
                            return false;
                        }
                    }
                    for (var leftEntity : leftPillar) {
                        var leftValueRange =
                                extractValueRangeFromEntity(scoreDirector, valueRangeDescriptor, leftEntity);
                        if (!leftValueRange.contains(rightValue)) {
                            return false;
                        }
                    }
                }
            }
        }
        return movable;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        for (var variableDescriptor : variableDescriptorList) {
            var oldLeftValue = variableDescriptor.getValue(leftPillar.getFirst());
            var oldRightValue = variableDescriptor.getValue(rightPillar.getFirst());
            if (!Objects.equals(oldLeftValue, oldRightValue)) {
                for (var leftEntity : leftPillar) {
                    scoreDirector.changeVariableFacade(variableDescriptor, leftEntity, oldRightValue);
                }
                for (var rightEntity : rightPillar) {
                    scoreDirector.changeVariableFacade(variableDescriptor, rightEntity, oldLeftValue);
                }
            }
        }
    }

    @Override
    public SelectorBasedPillarSwapMove<Solution_> rebase(Rebaser rebaser) {
        return new SelectorBasedPillarSwapMove<>(variableDescriptorList, rebaseList(leftPillar, rebaser),
                rebaseList(rightPillar, rebaser));
    }

    @Override
    public String describe() {
        var moveTypeDescription = new StringBuilder(20 * (variableDescriptorList.size() + 1));
        moveTypeDescription.append("PillarSwapMove(");
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
        return CollectionUtils.concat(leftPillar, rightPillar);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        List<Object> values = new ArrayList<>(variableDescriptorList.size() * 2);
        for (var variableDescriptor : variableDescriptorList) {
            values.add(variableDescriptor.getValue(leftPillar.getFirst()));
            values.add(variableDescriptor.getValue(rightPillar.getFirst()));
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
        final var other = (SelectorBasedPillarSwapMove<?>) o;
        return Objects.equals(variableDescriptorList, other.variableDescriptorList) &&
                Objects.equals(leftPillar, other.leftPillar) &&
                Objects.equals(rightPillar, other.rightPillar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptorList, leftPillar, rightPillar);
    }

    @Override
    public String toString() {
        var s = new StringBuilder(variableDescriptorList.size() * 16);
        s.append(leftPillar).append(" {");
        appendVariablesToString(s, leftPillar);
        s.append("} <-> ");
        s.append(rightPillar).append(" {");
        appendVariablesToString(s, rightPillar);
        s.append("}");
        return s.toString();
    }

    protected void appendVariablesToString(StringBuilder s, List<Object> pillar) {
        var first = true;
        for (var variableDescriptor : variableDescriptorList) {
            if (!first) {
                s.append(", ");
            }
            var value = variableDescriptor.getValue(pillar.getFirst());
            s.append(value == null ? null : value.toString());
            first = false;
        }
    }

}
