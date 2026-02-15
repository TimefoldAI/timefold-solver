package ai.timefold.solver.core.preview.api.move.builtin;

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
 * Moves an element of a {@link PlanningListVariable list variable}. The moved element is identified
 * by an entity instance and a position in that entity's list variable. The element is inserted at the given index
 * in the given destination entity's list variable.
 * <p>
 * An undo move is simply created by flipping the source and destination entity+index.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public class ListChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Entity_ sourceEntity;
    private final int sourceIndex;
    private final Entity_ destinationEntity;
    private final int destinationIndex;

    private @Nullable Value_ planningValue;

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
     * @param variableMetaModel never null
     * @param sourceEntity planning entity instance from which a planning value will be removed, for example "Ann"
     * @param sourceIndex index in sourceEntity's list variable from which a planning value will be removed
     * @param destinationEntity planning entity instance to which a planning value will be moved, for example "Bob"
     * @param destinationIndex index in destinationEntity's list variable where the moved planning value will be inserted
     */
    protected ListChangeMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity,
            int sourceIndex, Entity_ destinationEntity, int destinationIndex) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sourceEntity = Objects.requireNonNull(sourceEntity);
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("The sourceIndex (%d) must be >= 0."
                    .formatted(sourceIndex));
        }
        this.sourceIndex = sourceIndex;
        this.destinationEntity = Objects.requireNonNull(destinationEntity);
        if (destinationIndex < 0) {
            throw new IllegalArgumentException("The destinationIndex (%d) must be >= 0."
                    .formatted(destinationIndex));
        }
        this.destinationIndex = destinationIndex;
    }

    @SuppressWarnings("unchecked")
    private Value_ getMovedValue() {
        if (planningValue == null) {
            planningValue = (Value_) getVariableDescriptor(variableMetaModel)
                    .getValue(sourceEntity)
                    .get(sourceIndex);
        }
        return planningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        planningValue = (sourceEntity == destinationEntity)
                ? solutionView.moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex)
                : solutionView.moveValueBetweenLists(variableMetaModel, sourceEntity, sourceIndex, destinationEntity,
                        destinationIndex);
    }

    @Override
    public ListChangeMove<Solution_, Entity_, Value_> rebase(Rebaser rebaser) {
        return new ListChangeMove<>(variableMetaModel,
                Objects.requireNonNull(rebaser.rebase(sourceEntity)), sourceIndex,
                Objects.requireNonNull(rebaser.rebase(destinationEntity)), destinationIndex);
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        if (sourceEntity == destinationEntity) {
            return List.of(sourceEntity);
        } else {
            return List.of(sourceEntity, destinationEntity);
        }
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return List.of(getMovedValue());
    }

    @Override
    public List<PlanningListVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    public Entity_ getSourceEntity() {
        return sourceEntity;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public Entity_ getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ListChangeMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(sourceEntity, other.sourceEntity)
                && sourceIndex == other.sourceIndex
                && Objects.equals(destinationEntity, other.destinationEntity)
                && destinationIndex == other.destinationIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }

    @Override
    public String toString() {
        return String.format("%s {%s[%d] -> %s[%d]}",
                getMovedValue(), sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }
}
