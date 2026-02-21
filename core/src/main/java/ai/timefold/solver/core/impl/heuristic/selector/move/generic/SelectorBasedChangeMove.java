package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedChangeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected final Object entity;
    protected final @Nullable Object toPlanningValue;

    public SelectorBasedChangeMove(GenuineVariableDescriptor<Solution_> variableDescriptor, Object entity,
            @Nullable Object toPlanningValue) {
        this.variableDescriptor = variableDescriptor;
        this.entity = entity;
        this.toPlanningValue = toPlanningValue;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public Object getEntity() {
        return entity;
    }

    public @Nullable Object getToPlanningValue() {
        return toPlanningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        var oldValue = variableDescriptor.getValue(entity);
        return !Objects.equals(oldValue, toPlanningValue);
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.changeVariableFacade(variableDescriptor, entity, toPlanningValue);
    }

    @Override
    public SelectorBasedChangeMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedChangeMove<>(variableDescriptor, lookup.lookUpWorkingObject(entity),
                lookup.lookUpWorkingObject(toPlanningValue));
    }

    @Override
    public String describe() {
        return "ChangeMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return Collections.singletonList(entity);
    }

    @Override
    public SequencedCollection<@Nullable Object> getPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (SelectorBasedChangeMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(entity, other.entity) &&
                Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, entity, toPlanningValue);
    }

    @Override
    public String toString() {
        var oldValue = variableDescriptor.getValue(entity);
        return "%s {%s -> %s}"
                .formatted(entity, oldValue, toPlanningValue);
    }

}
