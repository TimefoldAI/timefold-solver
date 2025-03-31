package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NonNull;

/**
 * Moves an element of a {@link PlanningListVariable list variable}. The moved element is identified
 * by an entity instance and a position in that entity's list variable. The element is inserted at the given index
 * in the given destination entity's list variable.
 * <p>
 * An undo move is simply created by flipping the source and destination entity+index.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class ListChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Entity_ sourceEntity;
    private final int sourceIndex;
    private final Entity_ destinationEntity;
    private final int destinationIndex;

    private Value_ planningValue;

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
    public ListChangeMove(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity,
            int sourceIndex, Entity_ destinationEntity, int destinationIndex) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.sourceEntity = Objects.requireNonNull(sourceEntity);
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("The sourceIndex (" + sourceIndex + ") must be greater than 0.");
        }
        this.sourceIndex = sourceIndex;
        this.destinationEntity = Objects.requireNonNull(destinationEntity);
        if (destinationIndex < 0) {
            throw new IllegalArgumentException("The destinationIndex (" + destinationIndex + ") must be greater than 0.");
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
    public void execute(@NonNull MutableSolutionView<Solution_> solutionView) {
        if (sourceEntity == destinationEntity) {
            planningValue = solutionView.moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
        } else {
            planningValue = solutionView.moveValueBetweenLists(variableMetaModel, sourceEntity, sourceIndex, destinationEntity,
                    destinationIndex);
        }
    }

    @Override
    public @NonNull ListChangeMove<Solution_, Entity_, Value_> rebase(@NonNull Rebaser rebaser) {
        return new ListChangeMove<>(variableMetaModel,
                rebaser.rebase(sourceEntity), sourceIndex,
                rebaser.rebase(destinationEntity), destinationIndex);
    }

    @Override
    public @NonNull Collection<Object> extractPlanningEntities() {
        if (sourceEntity == destinationEntity) {
            return Collections.singleton(sourceEntity);
        } else {
            return List.of(sourceEntity, destinationEntity);
        }
    }

    @Override
    public @NonNull Collection<Object> extractPlanningValues() {
        return Collections.singleton(planningValue);
    }

    @Override
    protected List<VariableMetaModel<Solution_, ?, ?>> getVariableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ListChangeMove<?, ?, ?> that))
            return false;
        return sourceIndex == that.sourceIndex && destinationIndex == that.destinationIndex
                && Objects.equals(variableMetaModel, that.variableMetaModel) && Objects.equals(sourceEntity, that.sourceEntity)
                && Objects.equals(destinationEntity, that.destinationEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }

    @Override
    public @NonNull String toString() {
        return String.format("%s {%s[%d] -> %s[%d]}",
                getMovedValue(), sourceEntity, sourceIndex, destinationEntity, destinationIndex);
    }
}
