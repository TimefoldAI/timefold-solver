package ai.timefold.solver.core.impl.move.streams.generic.move;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class ChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    protected final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    protected final Entity_ entity;
    protected final @Nullable Value_ toPlanningValue;

    private @Nullable Value_ currentValue;

    public ChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            @Nullable Value_ toPlanningValue) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.entity = Objects.requireNonNull(entity);
        this.toPlanningValue = toPlanningValue;
    }

    protected @Nullable Value_ readValue(SolutionView<Solution_> solutionView) {
        if (currentValue == null) {
            currentValue = solutionView.getValue(variableMetaModel, entity);
        }
        return currentValue;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        solutionView.changeVariable(variableMetaModel, entity, toPlanningValue);
    }

    @Override
    public ChangeMove<Solution_, Entity_, Value_> rebase(Rebaser rebaser) {
        return new ChangeMove<>(variableMetaModel, Objects.requireNonNull(rebaser.rebase(entity)),
                rebaser.rebase(toPlanningValue));
    }

    @Override
    public Collection<Entity_> extractPlanningEntities() {
        return Collections.singletonList(entity);
    }

    @Override
    public Collection<Value_> extractPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    protected List<VariableMetaModel<Solution_, ?, ?>> getVariableMetaModels() {
        return List.of(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChangeMove<?, ?, ?> that &&
                Objects.equals(variableMetaModel, that.variableMetaModel) &&
                Objects.equals(entity, that.entity) &&
                Objects.equals(toPlanningValue, that.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, entity, toPlanningValue);
    }

    @Override
    public String toString() {
        var oldValue = currentValue == null ? getVariableDescriptor(variableMetaModel).getValue(entity) : currentValue;
        return entity + " {" + oldValue + " -> " + toPlanningValue + "}";
    }

}
