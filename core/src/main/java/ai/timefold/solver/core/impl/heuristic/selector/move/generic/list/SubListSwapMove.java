package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SubListSwapMove<Solution_> extends AbstractMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final SubList leftSubList;
    private final SubList rightSubList;
    private final boolean reversing;
    private final int rightFromIndex;
    private final int leftToIndex;

    private List<Object> leftPlanningValueList;
    private List<Object> rightPlanningValueList;

    public SubListSwapMove(ListVariableDescriptor<Solution_> variableDescriptor,
            Object leftEntity, int leftFromIndex, int leftToIndex,
            Object rightEntity, int rightFromIndex, int rightToIndex,
            boolean reversing) {
        this(variableDescriptor,
                new SubList(leftEntity, leftFromIndex, leftToIndex - leftFromIndex),
                new SubList(rightEntity, rightFromIndex, rightToIndex - rightFromIndex),
                reversing);
    }

    public SubListSwapMove(ListVariableDescriptor<Solution_> variableDescriptor,
            SubList leftSubList,
            SubList rightSubList,
            boolean reversing) {
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
        return leftSubList.entity() != rightSubList.entity() || rightFromIndex >= leftToIndex;
    }

    @Override
    protected AbstractMove<Solution_> createUndoMove(ScoreDirector<Solution_> scoreDirector) {
        if (leftSubList.entity() == rightSubList.entity()) {
            return new SubListSwapMove<>(variableDescriptor,
                    new SubList(leftSubList.entity(), leftSubList.fromIndex(), rightSubList.length()),
                    new SubList(rightSubList.entity(),
                            rightSubList.fromIndex() - leftSubList.length() + rightSubList.length(),
                            leftSubList.length()),
                    reversing);
        }
        return new SubListSwapMove<>(variableDescriptor,
                new SubList(rightSubList.entity(), rightSubList.fromIndex(), leftSubList.length()),
                new SubList(leftSubList.entity(), leftSubList.fromIndex(), rightSubList.length()),
                reversing);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;

        Object leftEntity = leftSubList.entity();
        Object rightEntity = rightSubList.entity();
        int leftSubListLength = leftSubList.length();
        int rightSubListLength = rightSubList.length();
        int leftFromIndex = leftSubList.fromIndex();
        List<Object> leftList = variableDescriptor.getValue(leftEntity);
        List<Object> rightList = variableDescriptor.getValue(rightEntity);
        List<Object> leftSubListView = subList(leftSubList);
        List<Object> rightSubListView = subList(rightSubList);
        leftPlanningValueList = CollectionUtils.copy(leftSubListView, reversing);
        rightPlanningValueList = CollectionUtils.copy(rightSubListView, reversing);

        if (leftEntity == rightEntity) {
            int fromIndex = Math.min(leftFromIndex, rightFromIndex);
            int toIndex = leftFromIndex > rightFromIndex
                    ? leftFromIndex + leftSubListLength
                    : rightFromIndex + rightSubListLength;
            int leftSubListDestinationIndex = rightFromIndex + rightSubListLength - leftSubListLength;
            innerScoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
            rightSubListView.clear();
            subList(leftSubList).clear();
            leftList.addAll(leftFromIndex, rightPlanningValueList);
            rightList.addAll(leftSubListDestinationIndex, leftPlanningValueList);
            innerScoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
        } else {
            innerScoreDirector.beforeListVariableChanged(variableDescriptor,
                    leftEntity, leftFromIndex, leftFromIndex + leftSubListLength);
            innerScoreDirector.beforeListVariableChanged(variableDescriptor,
                    rightEntity, rightFromIndex, rightFromIndex + rightSubListLength);
            rightSubListView.clear();
            leftSubListView.clear();
            leftList.addAll(leftFromIndex, rightPlanningValueList);
            rightList.addAll(rightFromIndex, leftPlanningValueList);
            innerScoreDirector.afterListVariableChanged(variableDescriptor,
                    leftEntity, leftFromIndex, leftFromIndex + rightSubListLength);
            innerScoreDirector.afterListVariableChanged(variableDescriptor,
                    rightEntity, rightFromIndex, rightFromIndex + leftSubListLength);
        }
    }

    @Override
    public SubListSwapMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new SubListSwapMove<>(
                variableDescriptor,
                leftSubList.rebase(destinationScoreDirector),
                rightSubList.rebase(destinationScoreDirector),
                reversing);
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
        Set<Object> entities = new LinkedHashSet<>(2);
        entities.add(leftSubList.entity());
        entities.add(rightSubList.entity());
        return entities;
    }

    @Override
    public Collection<Object> getPlanningValues() {
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
        SubListSwapMove<?> other = (SubListSwapMove<?>) o;
        return reversing == other.reversing && rightFromIndex == other.rightFromIndex && leftToIndex == other.leftToIndex
                && variableDescriptor.equals(other.variableDescriptor)
                && leftSubList.equals(other.leftSubList)
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
