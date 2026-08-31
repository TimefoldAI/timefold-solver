package ai.timefold.solver.core.impl.move;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.InnerGenuineVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed class MoveDirector<Solution_, Score_ extends Score<Score_>>
        implements InnerMutableSolutionView<Solution_>, Lookup permits EphemeralMoveDirector {

    protected final VariableDescriptorAwareScoreDirector<Solution_> externalScoreDirector;
    private final InnerScoreDirector<Solution_, Score_> backingScoreDirector;

    public MoveDirector(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        this.backingScoreDirector = Objects.requireNonNull(scoreDirector);
        if (EphemeralMoveDirector.class.isAssignableFrom(getClass())) {
            // Ephemeral move director records operations for a later undo,
            // and the external director is no longer an instance of InnerScoreDirector.
            // However, some pieces of code need methods from InnerScoreDirector,
            // in which case we turn to the backing score director.
            // This is only safe for operations that do not need to be undone,
            // such as calculateScore().
            // Operations which need undo must go through the external score director,
            // which is recording in this case.
            this.externalScoreDirector = new VariableChangeRecordingScoreDirector<>(scoreDirector, false);
        } else {
            this.externalScoreDirector = scoreDirector;
        }
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return backingScoreDirector.getSolutionDescriptor().getMetaModel();
    }

    @Override
    public final <Entity_, Value_> void assignValueAndAdd(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
            Entity_ destinationEntity, int destinationIndex) {
        if (!(getPositionOf(variableMetaModel, planningValue) instanceof UnassignedElement)) {
            throw new IllegalStateException("Cannot assign an already assigned value (%s).".formatted(planningValue));
        }
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.addElement(destinationEntity, destinationIndex, planningValue);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.updateShadowVariables();
    }

    @Override
    public <Entity_, Value_> void assignValuesAndAdd(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, List<Value_> values,
            Entity_ destinationEntity, int destinationIndex) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        for (var value : values) {
            if (!(getPositionOf(variableMetaModel, value) instanceof UnassignedElement)) {
                throw new IllegalStateException("Cannot assign an already assigned value (%s).".formatted(value));
            }
        }
        assignValuesAndAddElements(variableDescriptor, values, destinationEntity, destinationIndex);
        externalScoreDirector.updateShadowVariables();
    }

    private <Entity_, Value_> void assignValuesAndAddElements(ListVariableDescriptor<Solution_> variableDescriptor,
            List<Value_> values, Entity_ destinationEntity, int destinationIndex) {
        for (var value : values) {
            externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, value);
        }
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.getValue(destinationEntity).addAll(destinationIndex, values);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + values.size());
        for (var value : values) {
            externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, value);
        }
    }

    @Override
    public final <Entity_, Value_> void assignValueAndSet(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ planningValue,
            Entity_ destinationEntity, int destinationIndex) {
        if (destinationIndex == countValues(variableMetaModel, destinationEntity)) {
            // Faster code path, no need to unassign anything.
            assignValueAndAdd(variableMetaModel, planningValue, destinationEntity, destinationIndex);
            return;
        }

        if (!(getPositionOf(variableMetaModel, planningValue) instanceof UnassignedElement)) {
            throw new IllegalStateException("Cannot assign an already assigned value (%s).".formatted(planningValue));
        }

        var oldValue = getValueAtIndex(variableMetaModel, destinationEntity, destinationIndex);
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        externalScoreDirector.beforeListVariableElementAssigned(variableDescriptor, planningValue);
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, oldValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        var actualOldValue = variableDescriptor.setElement(destinationEntity, destinationIndex, planningValue);
        if (oldValue != actualOldValue) {
            throw new IllegalStateException(
                    "Impossible state: The value (%s) at index (%d) of entity (%s) is not as expected (%s)."
                            .formatted(actualOldValue, destinationIndex, destinationEntity, oldValue));
        }
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, oldValue);
        externalScoreDirector.afterListVariableElementAssigned(variableDescriptor, planningValue);

        externalScoreDirector.updateShadowVariables();
    }

    @Override
    public <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        var locationInList = getPositionOf(variableMetaModel, value).ensureAssigned(() -> """
                The value (%s) is not assigned to a list variable.
                This may indicate score corruption or a problem with the move's implementation.""".formatted(value));
        unassignValue(variableMetaModel, value, locationInList.entity(), locationInList.index());
    }

    @Override
    public <Entity_, Value_> Value_ unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index) {
        var value = getValueAtIndex(variableMetaModel, entity, index);
        unassignValue(variableMetaModel, value, entity, index);
        return value;
    }

    private <Entity_, Value_> void unassignValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ movedValue, Entity_ entity, int index) {
        var variableDescriptor =
                ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
        unassignValueElement(variableDescriptor, movedValue, entity, index);
        externalScoreDirector.updateShadowVariables();
    }

    private <Entity_, Value_> void unassignValueElement(ListVariableDescriptor<Solution_> variableDescriptor,
            Value_ movedValue, Entity_ entity, int index) {
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, movedValue);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, index, index + 1);
        variableDescriptor.getValue(entity).remove(index);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, index, index);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, movedValue);
    }

    @Override
    public final <Entity_, Value_> void changeVariable(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, @Nullable Value_ newValue) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        externalScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        externalScoreDirector.afterVariableChanged(variableDescriptor, entity);
        externalScoreDirector.updateShadowVariables();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ moveValueBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex) {
        if (sourceEntity == destinationEntity) {
            // Moving within the same list is not supported by this method.
            // This avoids confusion about the shifting of indices when removing and adding within the same list.
            throw new IllegalArgumentException(
                    "Source entity (%s) and destination entity (%s) must be different when moving values between lists."
                            .formatted(sourceEntity, destinationEntity));
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

        externalScoreDirector.updateShadowVariables();
        return element;
    }

    @Override
    public <Entity_, Value_> Value_ replaceValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, Entity_ destinationEntity, int destinationIndex) {
        if (destinationEntity == sourceEntity) {
            return replaceValue(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var toReplace = (Value_) variableDescriptor.getElement(destinationEntity, destinationIndex);
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, toReplace);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + 1);
        var toMove = variableDescriptor.removeElement(sourceEntity, sourceIndex);
        variableDescriptor.setElement(destinationEntity, destinationIndex, toMove);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + 1);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, toReplace);
        externalScoreDirector.updateShadowVariables();
        return toReplace;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ moveValueInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            throw new IllegalArgumentException(
                    "When moving values in the same list, sourceIndex (%d) and destinationIndex (%d) must be different."
                            .formatted(sourceIndex, destinationIndex));
        } else if (sourceIndex < 0 || destinationIndex < 0) {
            throw new IndexOutOfBoundsException("The sourceIndex (%d) and destinationIndex (%d) must both be >= 0."
                    .formatted(sourceIndex, destinationIndex));
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var list = variableDescriptor.getValue(sourceEntity);
        var listSize = list.size();
        if (sourceIndex >= listSize) {
            throw new IndexOutOfBoundsException("The sourceIndex (%d) must be less than the list size (%d)."
                    .formatted(sourceIndex, listSize));
        } else if (destinationIndex >= listSize) {
            throw new IndexOutOfBoundsException("The destinationIndex (%d) must be less than the list size (%d)."
                    .formatted(destinationIndex, listSize));
        }

        var fromIndex = Math.min(sourceIndex, destinationIndex);
        var toIndex = Math.max(sourceIndex, destinationIndex) + 1;
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        moveInList(list, sourceIndex, destinationIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        externalScoreDirector.updateShadowVariables();
        return (Value_) list.get(destinationIndex);
    }

    @Override
    public <Entity_, Value_> Value_ replaceValue(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int sourceIndex,
            int destinationIndex) {
        if (sourceIndex == destinationIndex) {
            throw new IllegalArgumentException(
                    "When replacing values in the same list, sourceIndex (%d) and destinationIndex (%d) must be different."
                            .formatted(sourceIndex, destinationIndex));
        } else if (sourceIndex < 0 || destinationIndex < 0) {
            throw new IndexOutOfBoundsException("The sourceIndex (%d) and destinationIndex (%d) must both be >= 0."
                    .formatted(sourceIndex, destinationIndex));
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var fromIndex = Math.min(sourceIndex, destinationIndex);
        var toIndex = Math.max(sourceIndex, destinationIndex) + 1;
        var list = variableDescriptor.getValue(entity);
        var toReplace = (Value_) list.get(destinationIndex);
        externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, toReplace);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        if (destinationIndex > sourceIndex) {
            // Remove from sourceIndex after setting the destination to preserve index validity.
            list.set(destinationIndex, list.get(sourceIndex));
            list.remove(sourceIndex);
        } else {
            list.set(destinationIndex, list.remove(sourceIndex));
        }
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex - 1);
        externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, toReplace);
        externalScoreDirector.updateShadowVariables();
        return toReplace;
    }

    /**
     * Moves the element at index {@code from} to index {@code to} in a list,
     * choosing the faster of two strategies based on the move's distance and position within the list.
     *
     * <p>
     * <b>Strategy selection</b> (lo = min(from, to), d = |from − to|):
     * <ul>
     * <li>Use {@code Collections.rotate} when {@code d * 8 < n − lo}
     * (distance is small relative to the remaining tail).</li>
     * <li>Use {@code remove + add} otherwise.</li>
     * </ul>
     *
     * <p>
     * <b>Why position matters</b>: {@code remove+add} shifts {@code (n−1−from) + (n−1−to)} elements in total.
     * When one endpoint is near the tail,
     * one of those copies is nearly free, making {@code remove+add} cheap even for large lists.
     * {@code rotate} always pays for the full sublist span,
     * so it only wins when that span is short relative to what {@code removeAdd} would have to copy.
     *
     * <p>
     * The threshold constant 8 was determined empirically by benchmarking on HotSpot with a microbenchmark
     * that performed moves of varying distances and positions within lists of varying sizes.
     *
     * @param list the list to mutate; assumes {@link ArrayList}
     * @param from index of the element to move
     * @param to index the element should occupy after the move
     */
    private static <T> void moveInList(List<T> list, int from, int to) {
        var distance = Math.abs(from - to);
        if (distance == 1) {
            Collections.swap(list, from, to);
            return;
        }
        var distanceTimesEight = distance * 8L; // Long prevents unlikely yet possible overflow.
        var lowerIndex = Math.min(from, to);
        var tailLength = list.size() - lowerIndex;
        if (distanceTimesEight < tailLength) {
            Collections.rotate(list.subList(lowerIndex, lowerIndex + distance + 1), from < to ? -1 : 1);
        } else {
            list.add(to, list.remove(from));
        }
    }

    @Override
    public <Entity_, Value_> Value_ shiftValue(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ sourceEntity, int sourceIndex, int offset) {
        if (offset == 0) {
            throw new IllegalArgumentException("The offset (%d) must not be zero.".formatted(offset));
        }
        var destinationIndex = sourceIndex + offset;
        return moveValueInList(variableMetaModel, sourceEntity, sourceIndex, destinationIndex);
    }

    @Override
    public <Entity_, Value_> void swapValuesBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        if (leftEntity == rightEntity) {
            swapValuesInList(variableMetaModel, leftEntity, leftIndex, rightIndex);
        } else {
            var variableDescriptor = extractVariableDescriptor(variableMetaModel);
            var leftElement = variableDescriptor.getElement(leftEntity, leftIndex);
            var rightElement = variableDescriptor.getElement(rightEntity, rightIndex);

            externalScoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            variableDescriptor.setElement(leftEntity, leftIndex, rightElement);
            variableDescriptor.setElement(rightEntity, rightIndex, leftElement);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftIndex, leftIndex + 1);
            externalScoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightIndex, rightIndex + 1);
            externalScoreDirector.updateShadowVariables();
        }
    }

    @Override
    public <Entity_, Value_> void swapValuesInList(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int leftIndex, int rightIndex) {
        if (leftIndex == rightIndex) {
            throw new IllegalArgumentException(
                    "When swapping values in the same list, leftIndex (%d) and rightIndex (%d) must be different."
                            .formatted(leftIndex, rightIndex));
        }

        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var fromIndex = Math.min(leftIndex, rightIndex);
        var toIndex = Math.max(leftIndex, rightIndex) + 1;
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        var list = variableDescriptor.getValue(entity);
        Collections.swap(list, leftIndex, rightIndex);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        externalScoreDirector.updateShadowVariables();
    }

    private static void requireNonEmptySpan(int fromIndex, int toIndex) {
        if (toIndex <= fromIndex) {
            throw new IllegalArgumentException("The toIndex (%d) must be greater than the fromIndex (%d)."
                    .formatted(toIndex, fromIndex));
        }
    }

    private static <Entity_> void requireValidDestinationIndex(int destinationIndex, int maximumDestinationIndex,
            Entity_ entity) {
        if (destinationIndex < 0 || destinationIndex > maximumDestinationIndex) {
            throw new IllegalArgumentException(
                    "The destinationIndex (%d) of entity (%s) must be between 0 and %d."
                            .formatted(destinationIndex, entity, maximumDestinationIndex));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> List<Value_> moveValuesInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            int fromIndex, int toIndex, int destinationIndex, boolean reversing) {
        requireNonEmptySpan(fromIndex, toIndex);
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var list = variableDescriptor.getValue(entity);
        var length = toIndex - fromIndex;
        requireValidDestinationIndex(destinationIndex, list.size() - length, entity);
        var planningValues = CollectionUtils.copy(list.subList(fromIndex, toIndex), reversing);

        var bracketFromIndex = Math.min(fromIndex, destinationIndex);
        var bracketToIndex = Math.max(fromIndex, destinationIndex) + length;
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, bracketFromIndex, bracketToIndex);
        list.subList(fromIndex, toIndex).clear();
        list.addAll(destinationIndex, planningValues);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, bracketFromIndex, bracketToIndex);
        externalScoreDirector.updateShadowVariables();
        return (List<Value_>) planningValues;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> List<Value_> moveValuesBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity,
            int sourceFromIndex, int sourceToIndex, Entity_ destinationEntity, int destinationIndex,
            boolean reversing) {
        if (sourceEntity == destinationEntity) {
            // Moving within the same list is not supported by this method.
            // This avoids confusion about the shifting of indices when removing and adding within the same list.
            throw new IllegalArgumentException(
                    "Source entity (%s) and destination entity (%s) must be different when moving values between lists."
                            .formatted(sourceEntity, destinationEntity));
        }
        requireNonEmptySpan(sourceFromIndex, sourceToIndex);
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var sourceList = variableDescriptor.getValue(sourceEntity);
        var length = sourceToIndex - sourceFromIndex;
        requireValidDestinationIndex(destinationIndex, variableDescriptor.getValue(destinationEntity).size(),
                destinationEntity);
        var planningValues = CollectionUtils.copy(sourceList.subList(sourceFromIndex, sourceToIndex), reversing);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceFromIndex, sourceToIndex);
        sourceList.subList(sourceFromIndex, sourceToIndex).clear();
        externalScoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceFromIndex, sourceFromIndex);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex);
        variableDescriptor.getValue(destinationEntity).addAll(destinationIndex, planningValues);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                destinationIndex + length);

        externalScoreDirector.updateShadowVariables();
        return (List<Value_>) planningValues;
    }

    @Override
    public <Entity_, Value_> void swapValuesInList(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            int leftFromIndex, int leftToIndex, int rightFromIndex, int rightToIndex, boolean reversing) {
        requireNonEmptySpan(leftFromIndex, leftToIndex);
        requireNonEmptySpan(rightFromIndex, rightToIndex);
        if (leftToIndex > rightFromIndex) {
            throw new IllegalArgumentException(
                    ("The leftToIndex (%d) must be less than or equal to the rightFromIndex (%d); the caller must order "
                            + "the left span before the right span.").formatted(leftToIndex, rightFromIndex));
        }
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var list = variableDescriptor.getValue(entity);
        var leftLength = leftToIndex - leftFromIndex;
        var leftPlanningValues = CollectionUtils.copy(list.subList(leftFromIndex, leftToIndex), reversing);
        var rightPlanningValues = CollectionUtils.copy(list.subList(rightFromIndex, rightToIndex), reversing);
        var leftDestinationIndex = rightToIndex - leftLength;

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, leftFromIndex, rightToIndex);
        // The right span is cleared first:
        // clearing it does not shift the left span's indices,
        // since the right span is entirely after the left one.
        // The left span must be re-derived as a fresh subList view after that clear,
        // since the earlier view of it was invalidated by the structural change to the backing list.
        list.subList(rightFromIndex, rightToIndex).clear();
        list.subList(leftFromIndex, leftToIndex).clear();
        list.addAll(leftFromIndex, rightPlanningValues);
        list.addAll(leftDestinationIndex, leftPlanningValues);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, leftFromIndex, rightToIndex);
        externalScoreDirector.updateShadowVariables();
    }

    @Override
    public <Entity_, Value_> void swapValuesBetweenLists(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity,
            int leftFromIndex, int leftToIndex, Entity_ rightEntity, int rightFromIndex, int rightToIndex,
            boolean reversing) {
        if (leftEntity == rightEntity) {
            throw new IllegalArgumentException("""
                    Left entity (%s) and right entity (%s) must be different when swapping spans between lists.
                    Use swapValuesInList(...) instead."""
                    .formatted(leftEntity, rightEntity));
        }
        requireNonEmptySpan(leftFromIndex, leftToIndex);
        requireNonEmptySpan(rightFromIndex, rightToIndex);
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var leftList = variableDescriptor.getValue(leftEntity);
        var rightList = variableDescriptor.getValue(rightEntity);
        var leftLength = leftToIndex - leftFromIndex;
        var rightLength = rightToIndex - rightFromIndex;
        var leftPlanningValues = CollectionUtils.copy(leftList.subList(leftFromIndex, leftToIndex), reversing);
        var rightPlanningValues = CollectionUtils.copy(rightList.subList(rightFromIndex, rightToIndex), reversing);

        externalScoreDirector.beforeListVariableChanged(variableDescriptor, leftEntity, leftFromIndex, leftToIndex);
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, rightEntity, rightFromIndex, rightToIndex);
        rightList.subList(rightFromIndex, rightToIndex).clear();
        leftList.subList(leftFromIndex, leftToIndex).clear();
        leftList.addAll(leftFromIndex, rightPlanningValues);
        rightList.addAll(rightFromIndex, leftPlanningValues);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, leftEntity, leftFromIndex,
                leftFromIndex + rightLength);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, rightEntity, rightFromIndex,
                rightFromIndex + leftLength);
        externalScoreDirector.updateShadowVariables();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> List<Value_> unassignValues(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            int fromIndex, int toIndex) {
        requireNonEmptySpan(fromIndex, toIndex);
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        var list = variableDescriptor.getValue(entity);
        var values = List.copyOf(list.subList(fromIndex, toIndex));
        for (var value : values) {
            externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value);
        }
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        list.subList(fromIndex, toIndex).clear();
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, fromIndex);
        for (var value : values) {
            externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, value);
        }
        externalScoreDirector.updateShadowVariables();
        return (List<Value_>) values;
    }

    @Override
    public <Entity_, Value_> List<Value_> massMoveValues(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Iterable<Value_> values,
            @Nullable PositionInList destination) {
        var variableDescriptor = extractVariableDescriptor(variableMetaModel);
        // One pass: materialize the values, and group every currently-assigned member's ORIGINAL index by its
        // source entity (one getPositionOf() call per value, not two). Reading positions inside a mutate-as-you-go
        // loop is WRONG - a member's live index can shift once an earlier same-entity member is removed.
        var valueList = new ArrayList<Value_>();
        Map<Entity_, BitSet> indexesByEntity = new LinkedHashMap<>();
        var removedBeforeDestination = 0;
        for (var value : values) {
            valueList.add(value);
            if (getPositionOf(variableMetaModel, value) instanceof PositionInList assigned) {
                Entity_ sourceEntity = assigned.entity();
                indexesByEntity.computeIfAbsent(sourceEntity, e -> new BitSet()).set(assigned.index());
                if (destination != null && sourceEntity == destination.entity() && assigned.index() < destination.index()) {
                    removedBeforeDestination++;
                }
            }
        }
        // One bracket per affected entity - not one per removed value - so the shadow-variable position rescan
        // that fires on every beforeListVariableChanged/afterListVariableChanged pair runs once per entity,
        // not once per sample member sharing that entity.
        for (var entry : indexesByEntity.entrySet()) {
            unassignValueElements(variableDescriptor, entry.getKey(), entry.getValue());
        }
        if (destination != null) {
            assignValuesAndAddElements(variableDescriptor, valueList, destination.<Entity_> entity(),
                    destination.index() - removedBeforeDestination);
        }
        externalScoreDirector.updateShadowVariables();
        return valueList;
    }

    /**
     * Removes the elements at the given (pre-removal) indexes from one entity's list variable, under a single
     * before/after bracket - honestly reporting the whole affected span, not just the removed indexes, so that
     * undo recording and the declarative shadow-variable graph (both of which trust the reported range to match
     * what actually changed) stay correct. Survivors within the span are removed and immediately reinserted in
     * their original relative order, exactly like {@link #unassignValues} already does when the entire span is
     * removed (the {@code indexes.cardinality() == span size} case this generalizes).
     */
    private <Entity_> void unassignValueElements(ListVariableDescriptor<Solution_> variableDescriptor, Entity_ entity,
            BitSet indexes) {
        var list = variableDescriptor.getValue(entity);
        var minIndex = indexes.nextSetBit(0);
        var maxIndexExclusive = indexes.length();
        var originalSpan = List.copyOf(list.subList(minIndex, maxIndexExclusive));
        var removedValues = new ArrayList<>(indexes.cardinality());
        var survivors = new ArrayList<>(originalSpan.size() - indexes.cardinality());
        for (var i = 0; i < originalSpan.size(); i++) {
            var value = originalSpan.get(i);
            if (indexes.get(minIndex + i)) {
                removedValues.add(value);
            } else {
                survivors.add(value);
            }
        }
        for (var value : removedValues) {
            externalScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, value);
        }
        externalScoreDirector.beforeListVariableChanged(variableDescriptor, entity, minIndex, maxIndexExclusive);
        list.subList(minIndex, maxIndexExclusive).clear();
        list.addAll(minIndex, survivors);
        externalScoreDirector.afterListVariableChanged(variableDescriptor, entity, minIndex, minIndex + survivors.size());
        for (var value : removedValues) {
            externalScoreDirector.afterListVariableElementUnassigned(variableDescriptor, value);
        }
    }

    @Override
    public <Entity_, Value_> ValueRange<Value_>
            getValueRange(GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, @Nullable Entity_ entity) {
        var innerGenuineVariableMetaModel = (InnerGenuineVariableMetaModel<Solution_>) variableMetaModel;
        var valueRangeDescriptor = innerGenuineVariableMetaModel.variableDescriptor().getValueRangeDescriptor();
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            return backingScoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor);
        } else {
            if (entity == null) {
                throw new IllegalArgumentException(
                        "The entity (null) cannot be null when the value range (%s) is defined on the entity."
                                .formatted(valueRangeDescriptor));
            }
            return backingScoreDirector.getValueRangeManager()
                    .getFromEntity(valueRangeDescriptor, entity);
        }
    }

    /**
     * Execute a given move and make sure shadow variables are up to date after that.
     * Does not run a score calculation.
     */
    public final void execute(Move<Solution_> move) {
        execute(move, false);
    }

    /**
     * Execute a given move and make sure shadow variables are up to date after that.
     *
     * @param guaranteeFreshScore if true, a score calculation is forced after executing the move,
     *        to ensure the score is up to date.
     */
    public final void execute(Move<Solution_> move, boolean guaranteeFreshScore) {
        executeAllowingStructurallyFlawedSolutions(move);
        if (!backingScoreDirector.isLastVariableUpdateSuccessful()) {
            throw new IllegalArgumentException("The move (%s) caused the solution to become structurally flawed."
                    .formatted(move));
        }
        if (guaranteeFreshScore) {
            backingScoreDirector.calculateScore();
        }
    }

    void executeAllowingStructurallyFlawedSolutions(Move<Solution_> move) {
        move.execute(this);
        externalScoreDirector.updateShadowVariables();
    }

    public final InnerScore<Score_> executeTemporary(Move<Solution_> move) {
        var solutionDescriptor = backingScoreDirector.getSolutionDescriptor();
        var workingSolution = backingScoreDirector.getWorkingSolution();
        var previousScore = solutionDescriptor.<Score_> getScore(workingSolution);
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.executeAllowingStructurallyFlawedSolutions(move);
        var score = backingScoreDirector.calculateScore();
        ephemeralMoveDirector.close(); // This undoes the move.
        // Restore the previous working score
        solutionDescriptor.setScore(workingSolution, previousScore);
        return score;
    }

    public @Nullable <Result_> Result_ executeTemporary(Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, @Nullable Result_> postprocessor) {
        var solutionDescriptor = backingScoreDirector.getSolutionDescriptor();
        var workingSolution = backingScoreDirector.getWorkingSolution();
        var previousScore = solutionDescriptor.<Score_> getScore(workingSolution);
        try (var ephemeralMoveDirector = ephemeral()) {
            ephemeralMoveDirector.executeAllowingStructurallyFlawedSolutions(move);
            var score = backingScoreDirector.calculateScore();
            return postprocessor.apply(score, ephemeralMoveDirector.createUndoMove());
        } finally {
            // Restore the previous working score
            solutionDescriptor.setScore(workingSolution, previousScore);
        }
    }

    public @Nullable <Result_> Result_ executeTemporary(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> postprocessor,
            boolean guaranteeFreshScore) {
        var solutionDescriptor = backingScoreDirector.getSolutionDescriptor();
        var workingSolution = backingScoreDirector.getWorkingSolution();
        var previousScore = solutionDescriptor.<Score_> getScore(workingSolution);
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move, true);
        var result = postprocessor.apply(backingScoreDirector.getWorkingSolution());
        ephemeralMoveDirector.close(); // This undoes the move.
        if (guaranteeFreshScore) {
            backingScoreDirector.calculateScore();
        } else {
            // Restore the previous working score
            solutionDescriptor.setScore(workingSolution, previousScore);
        }
        return result;
    }

    public @Nullable <Result_> Result_ executeTemporaryHandlingStructurallyFlawedSolutions(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> postprocessor,
            Function<Solution_, @Nullable Result_> flawedSolutionProcessor,
            boolean guaranteeFreshScore) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.executeAllowingStructurallyFlawedSolutions(move);
        var score = backingScoreDirector.calculateScore();
        Result_ result;
        if (score.isStructurallyFlawed()) {
            result = flawedSolutionProcessor.apply(backingScoreDirector.getWorkingSolution());
        } else {
            result = postprocessor.apply(backingScoreDirector.getWorkingSolution());
        }
        ephemeralMoveDirector.close(); // This undoes the move.
        if (guaranteeFreshScore) {
            backingScoreDirector.calculateScore();
        }
        return result;
    }

    /**
     * Like {@link #executeTemporary(Move, Function, boolean)}, but never calculates score -
     * not before {@code postprocessor} runs, not after, not at all.
     * Every other {@code executeTemporary} overload exists to learn the score effect of a move (that is the entire point of
     * trying a move temporarily in local search or exhaustive search),
     * so all of them pay for at least one {@code calculateScore()}.
     * This overload is for the rarer case of a caller that wants the move genuinely, temporarily applied -
     * so it can observe or exercise something other than score,
     * such as a neighborhood dataset network reacting to the change -
     * and has no use for the resulting score at all.
     * <p>
     * Because score is never touched, there is nothing to restore afterward either:
     * the working solution's score field is left exactly as the caller found it,
     * not merely restored to it.
     *
     * @return whatever {@code postprocessor} returns
     */
    public @Nullable <Result_> Result_ executeTemporaryWithoutScoring(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> postprocessor) {
        var ephemeralMoveDirector = ephemeral();
        ephemeralMoveDirector.execute(move);
        var result = postprocessor.apply(backingScoreDirector.getWorkingSolution());
        ephemeralMoveDirector.close(); // This undoes the move.
        return result;
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

    @Override
    public <Entity_, Value_> int getFirstUnpinnedIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity) {
        return extractVariableDescriptor(variableMetaModel).getFirstUnpinnedIndex(entity);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <Entity_, Value_> Value_ getValueAtIndex(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, int index) {
        return (Value_) extractVariableDescriptor(variableMetaModel).getValue(entity).get(index);
    }

    @Override
    public <Entity_, Value_> boolean isAssigned(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        return backingScoreDirector.getListVariableStateSupply(extractVariableDescriptor(variableMetaModel)).isAssigned(value);
    }

    @Override
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(backingScoreDirector, variableMetaModel, value);
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
        return !entityDescriptor.isMovable(backingScoreDirector.getWorkingSolution(), entity);
    }

    protected static <Solution_, Entity_, Value_> ElementPosition getPositionOf(InnerScoreDirector<Solution_, ?> scoreDirector,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableDescriptor, Value_ value) {
        return scoreDirector.getListVariableStateSupply(extractVariableDescriptor(listVariableDescriptor))
                .getElementPosition(value);
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
        return backingScoreDirector.getListVariableStateSupply(listVariableDescriptor).isPinned(value);
    }

    @Override
    public final <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity) {
        return externalScoreDirector.lookUpWorkingObject(problemFactOrPlanningEntity);
    }

    private static <Solution_, Entity_, Value_> BasicVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    private static <Solution_, Entity_, Value_> ListVariableDescriptor<Solution_>
            extractVariableDescriptor(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return ((DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel).variableDescriptor();
    }

    /**
     * Moves that are to be undone later need to be run with the instance returned by this method.
     * To undo the move, remember to call {@link EphemeralMoveDirector#close()}.
     *
     * @return never null
     */
    final EphemeralMoveDirector<Solution_, Score_> ephemeral() {
        return new EphemeralMoveDirector<>(backingScoreDirector);
    }

    @Override
    public final VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector() {
        return externalScoreDirector;
    }

    /**
     * Allows for reading data produced by a temporary move, before it is undone.
     * The score argument represents the score after executing the move on the solution.
     * The move argument represents the undo move for that move.
     *
     * @param <Solution_> type of the solution
     * @param <Score_> score of the move
     * @param <Result_> user-defined return type of the function
     */
    @FunctionalInterface
    public interface TemporaryMovePostprocessor<Solution_, Score_ extends Score<Score_>, Result_>
            extends BiFunction<InnerScore<Score_>, Move<Solution_>, @Nullable Result_> {

    }

}
