package ai.timefold.solver.core.impl.move.director;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerGenuineVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class DefaultMutableSolutionView<Solution_> implements InnerMutableSolutionView<Solution_>
        permits MoveDirector {
    protected final VariableDescriptorAwareScoreDirector<Solution_> externalScoreDirector;

    public DefaultMutableSolutionView(VariableDescriptorAwareScoreDirector<Solution_> externalScoreDirector) {
        this.externalScoreDirector = externalScoreDirector;
    }

    @Override
    public final <Entity_, Value_> void assignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ planningValue, Entity_ destinationEntity, int destinationIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
    }

    @Override
    public <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        var locationInList = getPositionOf(variableMetaModel, value)
                .ensureAssigned(() -> """
                        The value (%s) is not assigned to a list variable.
                        This may indicate score corruption or a problem with the move's implementation."""
                        .formatted(value));
        unassignValue(variableMetaModel, value, locationInList.entity(), locationInList.index());
    }

    @Override
    public <Entity_, Value_> Value_ unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index) {
        var value = getValueAtIndex(variableMetaModel, entity, index);
        unassignValue(variableMetaModel, value, entity, index);
        return value;
    }

    private <Entity_, Value_> void unassignValue(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ movedValue, Entity_ entity,
            int index) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, movedValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, index, index + 1);
        variableDescriptor.getValue(entity).remove(index);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, index, index);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, movedValue);
    }

    public final <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        externalScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        externalScoreDirector.afterVariableChanged(variableDescriptor, entity);
    }

    @SuppressWarnings("unchecked")
    public final <Entity_, Value_> @Nullable Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            return moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var element = (Value_) variableDescriptor.removeElement(sourceEntity, sourceIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, element);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);

        return element;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> @Nullable Value_ moveValueInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            return null;
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var fromIndex = Math.min(sourceIndex, destinationIndex);
        var toIndex = Math.max(sourceIndex, destinationIndex) + 1;

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        Value_ element = (Value_) variableDescriptor.removeElement(sourceEntity, sourceIndex);
        variableDescriptor.addElement(sourceEntity, destinationIndex, element);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);

        return element;
    }

    @Override
    public <Entity_, Value_> void swapValuesBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        if (leftEntity == rightEntity) {
            swapValuesInList(variableMetaModel, leftEntity, leftIndex, rightIndex);
        } else {
            var variableDescriptor = extractVariableDescriptor(variableMetaModel);
            externalScoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            var oldLeftElement = variableDescriptor.setElement(leftEntity, leftIndex,
                    variableDescriptor.getElement(rightEntity, rightIndex));
            variableDescriptor.setElement(rightEntity, rightIndex, oldLeftElement);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
        }
    }

    @Override
    public <Entity_, Value_> void swapValuesInList(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int leftIndex, int rightIndex) {
        if (leftIndex == rightIndex) {
            return;
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var fromIndex = Math.min(leftIndex, rightIndex);
        var toIndex = Math.max(leftIndex, rightIndex) + 1;

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        var oldLeftElement =
                variableDescriptor.setElement(entity, leftIndex, variableDescriptor.getElement(entity, rightIndex));
        variableDescriptor.setElement(entity, rightIndex, oldLeftElement);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public <Entity_, Value_> boolean isValueInRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity, @Nullable Value_ value) {
        var innerGenuineVariableMetaModel = (InnerGenuineVariableMetaModel<Solution_>) variableMetaModel;
        var valueRangeDescriptor = innerGenuineVariableMetaModel.variableDescriptor()
                .getValueRangeDescriptor();
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            return externalScoreDirector.getValueRangeManager()
                    .getFromSolution(valueRangeDescriptor)
                    .contains(value);
        } else {
            return externalScoreDirector.getValueRangeManager()
                    .getFromEntity(valueRangeDescriptor, Objects.requireNonNull(entity))
                    .contains(value);
        }
    }

    @Override
    public final <Entity_, Value_> Value_ getValue(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getValue(entity);
    }

    @Override
    public <Entity_, Value_> int countValues(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getValue(entity).size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ getValueAtIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(externalScoreDirector, variableMetaModel, value);
    }

    @Override
    public <Entity_, Value_> boolean isPinned(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Entity_ entity) {
        return isPinned(extractVariableDescriptor(variableMetaModel).getEntityDescriptor(), entity);
    }

    public <Value_> boolean isPinned(EntityDescriptor<Solution_> entityDescriptor, @Nullable Value_ entity) {
        if (entity == null) {
            return false; // Null is never pinned.
        }
        return !entityDescriptor.isMovable(externalScoreDirector.getWorkingSolution(), entity);
    }

    protected static <Solution_, Entity_, Value_> ElementPosition getPositionOf(
            VariableDescriptorAwareScoreDirector<Solution_> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableMetaModel, Value_ value) {
        var listVariableDescriptor = extractVariableDescriptor(listVariableMetaModel);
        return scoreDirector.getListVariableStateSupply(listVariableDescriptor).getElementPosition(value);
    }

    @Override
    public <Entity_, Value_> boolean isPinned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            @Nullable Value_ value) {
        return isPinned(extractVariableDescriptor(variableMetaModel), value);
    }

    public <Value_> boolean isPinned(ListVariableDescriptor<Solution_> listVariableDescriptor, @Nullable Value_ value) {
        if (value == null) {
            return false; // Null is never pinned.
        }
        return getListVariableStateSupply(listVariableDescriptor).isPinned(value);
    }

    @SuppressWarnings("unchecked")
    public final <Entity_, Value_> ListVariableStateSupply<Solution_, Entity_, Value_>
            getListVariableStateSupply(ListVariableDescriptor<Solution_> listVariableDescriptor) {
        return (ListVariableStateSupply<Solution_, Entity_, Value_>) externalScoreDirector
                .getListVariableStateSupply(listVariableDescriptor);
    }

    private static <Solution_, Entity_, Value_> BasicVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    private static <Solution_, Entity_, Value_> ListVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    @Override
    public VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return externalScoreDirector;
    }
}
