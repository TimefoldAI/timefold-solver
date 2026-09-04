package ai.timefold.solver.core.impl.move.builtin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.move.AbstractMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Changes the value of a {@link PlanningVariable} on every member of a {@link Sample} at once.
 * An assign is a move whose members currently hold null;
 * an unassign is a move whose {@link #toPlanningValue} is null.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public final class MassChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final @Nullable Value_ toPlanningValue;
    private final Sample<Entity_> sample;

    public MassChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Sample<Entity_> sample, @Nullable Value_ toPlanningValue) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.toPlanningValue = toPlanningValue;
        this.sample = Objects.requireNonNull(sample);
    }

    /**
     * @return the members whose variable this move changes
     */
    public Sample<Entity_> getSample() {
        return sample;
    }

    @Override
    public List<PlanningVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        for (var entity : sample) {
            solutionView.changeVariable(variableMetaModel, Objects.requireNonNull(entity), toPlanningValue);
        }
    }

    @Override
    public MassChangeMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        return new MassChangeMove<>(variableMetaModel, sample.rebase(lookup), lookup.lookUpWorkingObject(toPlanningValue));
    }

    @SuppressWarnings("unchecked")
    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return (SequencedCollection<Object>) sample.getMemberSet();
    }

    @Override
    public SequencedCollection<@Nullable Object> getPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MassChangeMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(toPlanningValue, other.toPlanningValue)
                && Objects.equals(sample, other.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, toPlanningValue, sample);
    }

    @Override
    public String toString() {
        return sample + " -> " + toPlanningValue;
    }

}
