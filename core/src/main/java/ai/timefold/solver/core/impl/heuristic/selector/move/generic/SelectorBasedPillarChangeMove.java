package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This {@link Move} is not cacheable.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class SelectorBasedPillarChangeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected final List<Object> pillar;
    protected final @Nullable Object toPlanningValue;

    public SelectorBasedPillarChangeMove(List<Object> pillar, GenuineVariableDescriptor<Solution_> variableDescriptor,
            @Nullable Object toPlanningValue) {
        this.pillar = pillar;
        this.variableDescriptor = variableDescriptor;
        this.toPlanningValue = toPlanningValue;
    }

    public List<Object> getPillar() {
        return pillar;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public @Nullable Object getToPlanningValue() {
        return toPlanningValue;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        var oldValue = variableDescriptor.getValue(pillar.getFirst());
        if (Objects.equals(oldValue, toPlanningValue)) {
            return false;
        }
        if (!variableDescriptor.canExtractValueRangeFromSolution()) {
            var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
            for (var entity : pillar) {
                var rightValueRange = extractValueRangeFromEntity(scoreDirector, valueRangeDescriptor, entity);
                if (!rightValueRange.contains(toPlanningValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        for (var entity : pillar) {
            scoreDirector.changeVariableFacade(variableDescriptor, entity, toPlanningValue);
        }
    }

    @Override
    public SelectorBasedPillarChangeMove<Solution_> rebase(Lookup lookup) {
        return new SelectorBasedPillarChangeMove<>(rebaseList(pillar, lookup), variableDescriptor,
                lookup.lookUpWorkingObject(toPlanningValue));
    }

    @Override
    public String describe() {
        return "PillarChangeMove(%s)"
                .formatted(variableDescriptor.getSimpleEntityAndVariableName());
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return pillar;
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
        var other = (SelectorBasedPillarChangeMove<?>) o;
        return Objects.equals(variableDescriptor, other.variableDescriptor) &&
                Objects.equals(pillar, other.pillar) &&
                Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor, pillar, toPlanningValue);
    }

    @Override
    public String toString() {
        var oldValue = variableDescriptor.getValue(pillar.getFirst());
        return "%s {%s -> %s}"
                .formatted(pillar, oldValue, toPlanningValue);
    }

}
