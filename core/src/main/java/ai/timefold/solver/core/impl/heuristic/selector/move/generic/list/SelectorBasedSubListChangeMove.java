package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedSubListChangeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final int length;
    private final Object destinationEntity;
    private final int destinationIndex;
    private final boolean reversing;

    private @Nullable SequencedCollection<Object> planningValues;

    public SelectorBasedSubListChangeMove(ListVariableDescriptor<Solution_> variableDescriptor, SubList subList,
            Object destinationEntity,
            int destinationIndex, boolean reversing) {
        this(variableDescriptor, subList.entity(), subList.fromIndex(), subList.length(), destinationEntity,
                destinationIndex, reversing);
    }

    public SelectorBasedSubListChangeMove(
            ListVariableDescriptor<Solution_> variableDescriptor,
            Object sourceEntity, int sourceIndex, int length,
            Object destinationEntity, int destinationIndex, boolean reversing) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
        this.length = length;
        this.destinationEntity = destinationEntity;
        this.destinationIndex = destinationIndex;
        this.reversing = reversing;
    }

    public Object getSourceEntity() {
        return sourceEntity;
    }

    public int getFromIndex() {
        return sourceIndex;
    }

    public int getSubListSize() {
        return length;
    }

    public int getToIndex() {
        return sourceIndex + length;
    }

    public boolean isReversing() {
        return reversing;
    }

    public Object getDestinationEntity() {
        return destinationEntity;
    }

    public int getDestinationIndex() {
        return destinationIndex;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        var sameEntity = Objects.equals(sourceEntity, destinationEntity);
        if (sameEntity && destinationIndex == sourceIndex) {
            return false;
        }
        var doable = !sameEntity || destinationIndex + length <= variableDescriptor.getListSize(destinationEntity);
        if (!doable || sameEntity || variableDescriptor.canExtractValueRangeFromSolution()) {
            return doable;
        }
        // When the first and second elements are different,
        // and the value range is located at the entity,
        // we need to check if the destination's value range accepts the upcoming values
        var valueRangeManager =
                ((VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector).getValueRangeManager();
        var destinationValueRange =
                valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(),
                        destinationEntity);
        var sourceList = variableDescriptor.getValue(sourceEntity);
        var subList = sourceList.subList(sourceIndex, sourceIndex + length);
        return subList.stream().allMatch(destinationValueRange::contains);
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        var sourceList = variableDescriptor.getValue(sourceEntity);
        var subList = sourceList.subList(sourceIndex, sourceIndex + length);
        planningValues = CollectionUtils.copy(subList, reversing);

        if (Objects.equals(sourceEntity, destinationEntity)) {
            var fromIndex = Math.min(sourceIndex, destinationIndex);
            var toIndex = Math.max(sourceIndex, destinationIndex) + length;
            scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
            subList.clear();
            variableDescriptor.getValue(destinationEntity).addAll(destinationIndex, planningValues);
            scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, fromIndex, toIndex);
        } else {
            scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex + length);
            subList.clear();
            scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
            scoreDirector.beforeListVariableChanged(variableDescriptor, destinationEntity, destinationIndex, destinationIndex);
            variableDescriptor.getValue(destinationEntity).addAll(destinationIndex, planningValues);
            scoreDirector.afterListVariableChanged(variableDescriptor, destinationEntity, destinationIndex,
                    destinationIndex + length);
        }
    }

    @Override
    public SelectorBasedSubListChangeMove<Solution_> rebase(Rebaser rebaser) {
        return new SelectorBasedSubListChangeMove<>(variableDescriptor,
                Objects.requireNonNull(rebaser.rebase(sourceEntity)), sourceIndex, length,
                Objects.requireNonNull(rebaser.rebase(destinationEntity)), destinationIndex, reversing);
    }

    @Override
    public String describe() {
        return "SubListChangeMove(%s)"
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
        return planningValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedSubListChangeMove<?>) o;
        return sourceIndex == other.sourceIndex && length == other.length
                && destinationIndex == other.destinationIndex && reversing == other.reversing
                && variableDescriptor.equals(other.variableDescriptor)
                && sourceEntity.equals(other.sourceEntity)
                && destinationEntity.equals(other.destinationEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex, length, destinationEntity, destinationIndex,
                reversing);
    }

    @Override
    public String toString() {
        return String.format("|%d| {%s[%d..%d] -%s> %s[%d]}",
                length, sourceEntity, sourceIndex, getToIndex(),
                reversing ? "reversing-" : "", destinationEntity, destinationIndex);
    }
}
