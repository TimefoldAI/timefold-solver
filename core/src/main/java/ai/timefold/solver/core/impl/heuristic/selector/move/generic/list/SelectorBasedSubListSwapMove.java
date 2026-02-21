package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedSubListSwapMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final SubList leftSubList;
    private final SubList rightSubList;
    private final boolean reversing;
    private final int rightFromIndex;
    private final int leftToIndex;

    private @Nullable List<Object> leftPlanningValueList;
    private @Nullable List<Object> rightPlanningValueList;

    public SelectorBasedSubListSwapMove(ListVariableDescriptor<Solution_> variableDescriptor, Object leftEntity,
            int leftFromIndex, int leftToIndex, Object rightEntity, int rightFromIndex, int rightToIndex, boolean reversing) {
        this(variableDescriptor, new SubList(leftEntity, leftFromIndex, leftToIndex - leftFromIndex),
                new SubList(rightEntity, rightFromIndex, rightToIndex - rightFromIndex), reversing);
    }

    public SelectorBasedSubListSwapMove(ListVariableDescriptor<Solution_> variableDescriptor, SubList leftSubList,
            SubList rightSubList, boolean reversing) {
        this.variableDescriptor = variableDescriptor;
        if (leftSubList.entity() == rightSubList.entity() && leftSubList.fromIndex() > rightSubList.fromIndex()) {
            this.leftSubList = rightSubList;
            this.rightSubList = leftSubList;
        } else {
            this.leftSubList = leftSubList;
            this.rightSubList = rightSubList;
        }
        this.reversing = reversing;
        rightFromIndex = this.rightSubList.fromIndex();
        leftToIndex = this.leftSubList.getToIndex();
    }

    public SubList getLeftSubList() {
        return leftSubList;
    }

    public SubList getRightSubList() {
        return rightSubList;
    }

    public boolean isReversing() {
        return reversing;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        // If both subLists are on the same entity, then they must not overlap.
        var doable = leftSubList.entity() != rightSubList.entity() || rightFromIndex >= leftToIndex;
        if (!doable || variableDescriptor.canExtractValueRangeFromSolution()) {
            return doable;
        }
        // When the left and right elements are different,
        // and the value range is located at the entity,
        // we need to check if the destination's value range accepts the upcoming values
        var valueRangeManager =
                ((VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector).getValueRangeManager();
        var leftEntity = leftSubList.entity();
        var leftList = subList(leftSubList);
        var leftValueRange = valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(), leftEntity);
        var rightEntity = rightSubList.entity();
        var rightList = subList(rightSubList);
        var rightValueRange = valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(), rightEntity);
        return leftList.stream().allMatch(rightValueRange::contains) && rightList.stream().allMatch(leftValueRange::contains);
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        var leftEntity = leftSubList.entity();
        var rightEntity = rightSubList.entity();
        var leftSubListLength = leftSubList.length();
        var rightSubListLength = rightSubList.length();
        var leftFromIndex = leftSubList.fromIndex();
        var leftList = variableDescriptor.getValue(leftEntity);
        var rightList = variableDescriptor.getValue(rightEntity);
        var leftSubListView = subList(leftSubList);
        var rightSubListView = subList(rightSubList);
        leftPlanningValueList = CollectionUtils.copy(leftSubListView, reversing);
        rightPlanningValueList = CollectionUtils.copy(rightSubListView, reversing);

        if (leftEntity == rightEntity) {
            var fromIndex = Math.min(leftFromIndex, rightFromIndex);
            var toIndex =
                    leftFromIndex > rightFromIndex ? leftFromIndex + leftSubListLength : rightFromIndex + rightSubListLength;
            var leftSubListDestinationIndex = rightFromIndex + rightSubListLength - leftSubListLength;
            scoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
            rightSubListView.clear();
            subList(leftSubList).clear();
            leftList.addAll(leftFromIndex, rightPlanningValueList);
            rightList.addAll(leftSubListDestinationIndex, leftPlanningValueList);
            scoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
        } else {
            scoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftFromIndex,
                    leftFromIndex + leftSubListLength);
            scoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightFromIndex,
                    rightFromIndex + rightSubListLength);
            rightSubListView.clear();
            leftSubListView.clear();
            leftList.addAll(leftFromIndex, rightPlanningValueList);
            rightList.addAll(rightFromIndex, leftPlanningValueList);
            scoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftFromIndex,
                    leftFromIndex + rightSubListLength);
            scoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightFromIndex,
                    rightFromIndex + leftSubListLength);
        }
    }

    @Override
    public SelectorBasedSubListSwapMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedSubListSwapMove<>(variableDescriptor, leftSubList.rebase(lookup), rightSubList.rebase(lookup),
                reversing);
    }

    @Override
    public String describe() {
        return "SubListSwapMove(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        var entities = LinkedHashSet.newLinkedHashSet(2);
        entities.add(leftSubList.entity());
        entities.add(rightSubList.entity());
        return entities;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return CollectionUtils.concat(leftPlanningValueList, rightPlanningValueList);
    }

    private List<Object> subList(SubList subList) {
        return variableDescriptor.getValue(subList.entity()).subList(subList.fromIndex(), subList.getToIndex());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedSubListSwapMove<?>) o;
        return reversing == other.reversing && rightFromIndex == other.rightFromIndex && leftToIndex == other.leftToIndex
                && variableDescriptor.equals(other.variableDescriptor) && leftSubList.equals(other.leftSubList)
                && rightSubList.equals(other.rightSubList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, leftSubList, rightSubList, reversing, rightFromIndex, leftToIndex);
    }

    @Override
    public String toString() {
        return "{" + leftSubList + "} <-" + (reversing ? "reversing-" : "") + "> {" + rightSubList + "}";
    }
}
