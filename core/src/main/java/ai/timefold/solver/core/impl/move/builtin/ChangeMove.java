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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @param <Value_> the variable type, the type of the property with the {@link PlanningVariable} annotation
 */
@NullMarked
public final class ChangeMove<Solution_, Entity_, Value_> extends AbstractMove<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Entity_ entity;
    private final @Nullable Value_ toPlanningValue;

    private @Nullable Value_ currentValue;
    private boolean currentValueCached = false;

    public ChangeMove(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            @Nullable Value_ toPlanningValue) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.entity = Objects.requireNonNull(entity);
        this.toPlanningValue = toPlanningValue;
    }

    @Nullable
    private Value_ getValue() {
        if (!currentValueCached) {
            currentValue = getVariableDescriptor(variableMetaModel).getValue(entity);
            currentValueCached = true;
        }
        return currentValue;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        getValue(); // Cache the current value if not already cached.
        solutionView.changeVariable(variableMetaModel, entity, toPlanningValue);
    }

    @Override
    public ChangeMove<Solution_, Entity_, Value_> rebase(Lookup lookup) {
        return new ChangeMove<>(variableMetaModel, lookup.lookUpNonNullWorkingObject(entity),
                lookup.lookUpWorkingObject(toPlanningValue));
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
