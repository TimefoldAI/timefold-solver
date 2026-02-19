package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Moves an element of a {@link PlanningListVariable list variable}. The moved element is identified
 * by an entity instance and a position in that entity's list variable. The element is inserted at the given index
 * in the given destination entity's list variable.
 * <p>
 * An undo move is simply created by flipping the source and destination entity+index.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedListChangeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final Object destinationEntity;
    private final int destinationIndex;

    private @Nullable Object planningValue;

    /**
     * The move removes a planning value element from {@code sourceEntity.listVariable[sourceIndex]}
     * and inserts the planning value at {@code destinationEntity.listVariable[destinationIndex]}.
     *
     * <h4>ListChangeMove anatomy</h4>
     *
     * <pre>
     * {@code
     *                             / destinationEntity
     *                             |   / destinationIndex
     *                             |   |
     *                A {Ann[0]}->{Bob[2]}
     *                |  |   |
     * planning value /  |   \ sourceIndex
     *                   \ sourceEntity
     * }
     * </pre>
     *
     * <h4>Example 1 - source and destination entities are different</h4>
     *
     * <pre>
     * {@code
     * GIVEN
     * Ann.tasks = [A, B, C]
     * Bob.tasks = [X, Y]
     *
     * WHEN
     * ListChangeMove: A {Ann[0]->Bob[2]}
     *
     * THEN
     * Ann.tasks = [B, C]
     * Bob.tasks = [X, Y, A]
     * }
     * </pre>
     *
     * <h4>Example 2 - source and destination is the same entity</h4>
     *
     * <pre>
     * {@code
     * GIVEN
     * Ann.tasks = [A, B, C]
     *
     * WHEN
     * ListChangeMove: A {Ann[0]->Ann[2]}
     *
     * THEN
     * Ann.tasks = [B, C, A]
     * }
     * </pre>
     *
     * @param variableDescriptor descriptor of a list variable, for example {@code Employee.taskList}
     * @param sourceEntity planning entity instance from which a planning value will be removed, for example "Ann"
     * @param sourceIndex index in sourceEntity's list variable from which a planning value will be removed
     * @param destinationEntity planning entity instance to which a planning value will be moved, for example "Bob"
     * @param destinationIndex index in destinationEntity's list variable where the moved planning value will be inserted
     */
    public SelectorBasedListChangeMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity,
            int sourceIndex,
            Object destinationEntity, int destinationIndex) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
        this.destinationEntity = destinationEntity;
        this.destinationIndex = destinationIndex;
    }

    public Object getSourceEntity() {
        return sourceEntity;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public Object getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    public Object getMovedValue() {
        if (planningValue == null) {
            planningValue = variableDescriptor.getElement(sourceEntity, sourceIndex);
        }
        return planningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return !Objects.equals(sourceEntity, destinationEntity)
                || (destinationIndex != sourceIndex && destinationIndex != variableDescriptor.getListSize(sourceEntity));
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        if (Objects.equals(sourceEntity, destinationEntity)) {
            var fromIndex = Math.min(sourceIndex, destinationIndex);
            var toIndex = Math.max(sourceIndex, destinationIndex) + 1;
            scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
            var element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
            variableDescriptor.addElement(destinationEntity, destinationIndex, element);
            scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
            planningValue = element;
        } else {
            scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
            var element = variableDescriptor.removeElement(sourceEntity, sourceIndex);
            scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

            scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex);
            variableDescriptor.addElement(destinationEntity, destinationIndex, element);
            scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                    destinationIndex + 1);
            planningValue = element;
        }
    }

    @Override
    public SelectorBasedListChangeMove<Solution_> rebase(Rebaser rebaser) {
        return new SelectorBasedListChangeMove<>(variableDescriptor, Objects.requireNonNull(rebaser.rebase(sourceEntity)),
                sourceIndex, Objects.requireNonNull(rebaser.rebase(destinationEntity)), destinationIndex);
    }

    @Override
    public String describe() {
        return "ListChangeMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // Use LinkedHashSet for predictable iteration order.
        var entities = LinkedHashSet.newLinkedHashSet(2);
        entities.add(sourceEntity);
        entities.add(destinationEntity);
        return entities;
    }

    @Override
    public SequencedCollection<@Nullable Object> getPlanningValues() {
        return Collections.singletonList(planningValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedListChangeMove<?>) o;
        return sourceIndex == other.sourceIndex && destinationIndex == other.destinationIndex
                && Objects.equals(variableDescriptor, other.variableDescriptor)
                && Objects.equals(sourceEntity, other.sourceEntity)
                && Objects.equals(destinationEntity, other.destinationEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d] -> %s[%d]}",
                getMovedValue(), sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }
}
