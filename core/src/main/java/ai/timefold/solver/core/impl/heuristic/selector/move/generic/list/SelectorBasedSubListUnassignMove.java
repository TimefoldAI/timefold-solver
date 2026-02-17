package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

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
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedSubListUnassignMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    private final ListVariableDescriptor<Solution_> variableDescriptor;
    private final Object sourceEntity;
    private final int sourceIndex;
    private final int length;

    private @Nullable SequencedCollection<Object> planningValues;

    public SelectorBasedSubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, SubList subList) {
        this(variableDescriptor, subList.entity(), subList.fromIndex(), subList.length());
    }

    private SelectorBasedSubListUnassignMove(ListVariableDescriptor<Solution_> variableDescriptor, Object sourceEntity,
            int sourceIndex, int length) {
        this.variableDescriptor = variableDescriptor;
        this.sourceEntity = sourceEntity;
        this.sourceIndex = sourceIndex;
        this.length = length;
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

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        if (sourceIndex < 0) {
            return false;
        }
        return variableDescriptor.getListSize(sourceEntity) >= getToIndex();
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        var sourceList = variableDescriptor.getValue(sourceEntity);
        var subList = sourceList.subList(sourceIndex, getToIndex());
        planningValues = List.copyOf(subList);

        for (var element : subList) {
            scoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
            scoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
        }
        scoreDirector.beforeListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, getToIndex());
        subList.clear();
        scoreDirector.afterListVariableChanged(variableDescriptor, sourceEntity, sourceIndex, sourceIndex);
    }

    @Override
    public Move<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedSubListUnassignMove<>(variableDescriptor, lookup.lookUpWorkingObject(sourceEntity), sourceIndex,
                length);
    }

    @Override
    public String describe() {
        return "SubListUnassignMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(sourceEntity);
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        return planningValues;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SelectorBasedSubListUnassignMove<?> other) {
            return sourceIndex == other.sourceIndex && length == other.length
                    && variableDescriptor.equals(other.variableDescriptor)
                    && sourceEntity.equals(other.sourceEntity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, sourceEntity, sourceIndex, length);
    }

    @Override
    public String toString() {
        return String.format("|%d| {%s[%d..%d] -> null}",
                length, sourceEntity, sourceIndex, getToIndex());
    }
}
