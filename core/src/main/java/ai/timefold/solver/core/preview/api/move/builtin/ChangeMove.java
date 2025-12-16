package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public class ChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    protected final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    protected final Entity_ entity;
    protected final @Nullable Value_ toPlanningValue;

    private @Nullable Value_ currentValue;

    protected ChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            @Nullable Value_ toPlanningValue) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.entity = Objects.requireNonNull(entity);
        this.toPlanningValue = toPlanningValue;
    }

    protected @Nullable Value_ getValue() {
        if (currentValue == null) {
            currentValue = getVariableDescriptor(variableMetaModel).getValue(entity);
        }
        return currentValue;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        getValue(); // Cache the current value if not already cached.
        solutionView.changeVariable(variableMetaModel, entity, toPlanningValue);
    }

    @Override
    public ChangeMove<Solution_, Entity_, Value_> rebase(Rebaser rebaser) {
        return new ChangeMove<>(variableMetaModel, Objects.requireNonNull(rebaser.rebase(entity)),
                rebaser.rebase(toPlanningValue));
    }

    @Override
    public Collection<Entity_> getPlanningEntities() {
        return Collections.singletonList(entity);
    }

    @Override
    public Collection<Value_> getPlanningValues() {
        return Collections.singletonList(toPlanningValue);
    }

    @Override
    public List<PlanningVariableMetaModel<Solution_, Entity_, Value_>> variableMetaModels() {
        return Collections.singletonList(variableMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChangeMove<?, ?, ?> other
                && Objects.equals(variableMetaModel, other.variableMetaModel)
                && Objects.equals(entity, other.entity)
                && Objects.equals(toPlanningValue, other.toPlanningValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, entity, toPlanningValue);
    }

    @Override
    public String toString() {
        return entity + " {" + getValue() + " -> " + toPlanningValue + "}";
    }

}
