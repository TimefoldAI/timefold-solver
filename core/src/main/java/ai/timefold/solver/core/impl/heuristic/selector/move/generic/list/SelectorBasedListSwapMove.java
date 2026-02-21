package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * Swaps two elements of a {@link PlanningListVariable list variable}.
 * Each element is identified by an entity instance and an index in that entity's list variable.
 * The swap move has two sides called left and right. The element at {@code leftIndex} in {@code leftEntity}'s list variable
 * is replaced by the element at {@code rightIndex} in {@code rightEntity}'s list variable and vice versa.
 * Left and right entity can be the same instance.
 * <p>
 * An undo move is created by flipping the left and right-hand entity and index.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedListSwapMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object leftEntity;
    private final int leftIndex;
    private final Object rightEntity;
    private final int rightIndex;

    /**
     * Create a move that swaps a list variable element at {@code leftEntity.listVariable[leftIndex]} with
     * {@code rightEntity.listVariable[rightIndex]}.
     *
     * <h4>ListSwapMove anatomy</h4>
     *
     * <pre>
     * {@code
     *                                 / rightEntity
     *                right element \  |   / rightIndex
     *                              |  |   |
     *               A {Ann[0]} <-> Y {Bob[1]}
     *               |  |   |
     *  left element /  |   \ leftIndex
     *                  \ leftEntity
     * }
     * </pre>
     *
     * <h4>Example</h4>
     *
     * <pre>
     * {@code
     * GIVEN
     * Ann.tasks = [A, B, C]
     * Bob.tasks = [X, Y]
     *
     * WHEN
     * ListSwapMove: A {Ann[0]} <-> Y {Bob[1]}
     *
     * THEN
     * Ann.tasks = [Y, B, C]
     * Bob.tasks = [X, A]
     * }
     * </pre>
     *
     * @param variableDescriptor descriptor of a list variable, for example {@code Employee.taskList}
     * @param leftEntity together with {@code leftIndex} identifies the left element to be moved
     * @param leftIndex together with {@code leftEntity} identifies the left element to be moved
     * @param rightEntity together with {@code rightIndex} identifies the right element to be moved
     * @param rightIndex together with {@code rightEntity} identifies the right element to be moved
     */
    public SelectorBasedListSwapMove(ListVariableDescriptor<Solution_> variableDescriptor, Object leftEntity, int leftIndex,
            Object rightEntity, int rightIndex) {
        this.variableDescriptor = variableDescriptor;
        this.leftEntity = leftEntity;
        this.leftIndex = leftIndex;
        this.rightEntity = rightEntity;
        this.rightIndex = rightIndex;
    }

    public Object getLeftEntity() {
        return leftEntity;
    }

    public int getLeftIndex() {
        return leftIndex;
    }

    public Object getRightEntity() {
        return rightEntity;
    }

    public int getRightIndex() {
        return rightIndex;
    }

    public Object getLeftValue() {
        return variableDescriptor.getElement(leftEntity, leftIndex);
    }

    public Object getRightValue() {
        return variableDescriptor.getElement(rightEntity, rightIndex);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return !(Objects.equals(leftEntity, rightEntity) && leftIndex == rightIndex);
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        var leftElement = variableDescriptor.getElement(leftEntity, leftIndex);
        var rightElement = variableDescriptor.getElement(rightEntity, rightIndex);

        if (Objects.equals(leftEntity, rightEntity)) {
            var fromIndex = Math.min(leftIndex, rightIndex);
            var toIndex = Math.max(leftIndex, rightIndex) + 1;
            scoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
            variableDescriptor.setElement(leftEntity, leftIndex, rightElement);
            variableDescriptor.setElement(rightEntity, rightIndex, leftElement);
            scoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, fromIndex, toIndex);
        } else {
            scoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            scoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            variableDescriptor.setElement(leftEntity, leftIndex, rightElement);
            variableDescriptor.setElement(rightEntity, rightIndex, leftElement);
            scoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            scoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
        }
    }

    @Override
    public SelectorBasedListSwapMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedListSwapMove<>(variableDescriptor, lookup.lookUpWorkingObject(leftEntity), leftIndex,
                lookup.lookUpWorkingObject(rightEntity), rightIndex);
    }

    @Override
    public String describe() {
        return "ListSwapMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        var entities = LinkedHashSet.newLinkedHashSet(2);
        entities.add(leftEntity);
        entities.add(rightEntity);
        return entities;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.of(getLeftValue(), getRightValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedListSwapMove<?>) o;
        return leftIndex == other.leftIndex && rightIndex == other.rightIndex
                && Objects.equals(variableDescriptor, other.variableDescriptor)
                && Objects.equals(leftEntity, other.leftEntity)
                && Objects.equals(rightEntity, other.rightEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d]} <-> %s {%s[%d]}",
                getLeftValue(), leftEntity, leftIndex,
                getRightValue(), rightEntity, rightIndex);
    }
}
