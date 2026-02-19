package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.move.AbstractMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Swaps two elements of a {@link PlanningListVariable list variable}.
 * Each element is identified by an entity instance and an index in that entity's list variable.
 * The swap move has two sides called left and right.
 * The element at {@code leftIndex} in {@code leftEntity}'s list variable
 * is replaced by the element at {@code rightIndex} in {@code rightEntity}'s list variable and vice versa.
 * Left and right entity can be the same instance.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public class ListSwapMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Entity_ leftEntity;
    private final int leftIndex;
    private final Entity_ rightEntity;
    private final int rightIndex;

    /**
     * Cache of the values of the entities
     * at the time of the first call of {@link #getLeftValueCache()} and {@link #getRightValueCache()} respectively.
     * Ideally, the method would first be called before the values are changed by the move,
     * so that the {@link #toString()} method shows the original values.
     */
    private @Nullable Value_ leftValue;
    private @Nullable Value_ rightValue;

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
     * @param variableMetaModel describes the variable to be changed by this move
     * @param leftEntity together with {@code leftIndex} identifies the left element to be moved
     * @param leftIndex together with {@code leftEntity} identifies the left element to be moved
     * @param rightEntity together with {@code rightIndex} identifies the right element to be moved
     * @param rightIndex together with {@code rightEntity} identifies the right element to be moved
     */
    protected ListSwapMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity,
            int leftIndex, Entity_ rightEntity, int rightIndex) {
        this.variableMetaModel = variableMetaModel;
        this.leftEntity = leftEntity;
        this.leftIndex = leftIndex;
        this.rightEntity = rightEntity;
        this.rightIndex = rightIndex;
    }

    public Entity_ getLeftEntity() {
        return leftEntity;
    }

    public int getLeftIndex() {
        return leftIndex;
    }

    public Entity_ getRightEntity() {
        return rightEntity;
    }

    public int getRightIndex() {
        return rightIndex;
    }

    private Value_ getLeftValueCache() {
        if (leftValue == null) {
            leftValue = getVariableDescriptor(variableMetaModel).getElement(leftEntity, leftIndex);
        }
        return leftValue;
    }

    private Value_ getRightValueCache() {
        if (rightValue == null) {
            rightValue = getVariableDescriptor(variableMetaModel).getElement(rightEntity, rightIndex);
        }
        return rightValue;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        solutionView.swapValuesBetweenLists(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    @Override
    public ListSwapMove<Solution_, Entity_, Value_> rebase(Rebaser rebaser) {
        return new ListSwapMove<>(variableMetaModel, rebaser.rebase(leftEntity), leftIndex, rebaser.rebase(rightEntity),
                rightIndex);
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return Collections.singletonList(variableMetaModel);
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return leftEntity == rightEntity ? Collections.singletonList(leftEntity) : Arrays.asList(leftEntity, rightEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return Arrays.asList(getLeftValueCache(), getRightValueCache());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ListSwapMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(leftEntity, other.leftEntity)
                && leftIndex == other.leftIndex
                && Objects.equals(rightEntity, other.rightEntity)
                && rightIndex == other.rightIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d]} <-> %s {%s[%d]}",
                getLeftValueCache(), leftEntity, leftIndex,
                getRightValueCache(), rightEntity, rightIndex);
    }

}
