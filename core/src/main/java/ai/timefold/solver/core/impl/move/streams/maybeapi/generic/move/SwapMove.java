package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 */
@NullMarked
public final class SwapMove<Solution_, Entity_> extends AbstractMove<Solution_> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final Entity_ leftEntity;
    private final Entity_ rightEntity;

    /**
     * Cache of the values of the entities at the time of the first call of {@link #getCachedValues()}.
     * Ideally, the method would first be called before the values are changed by the move,
     * so that the {@link #toString()} method shows the original values.
     * <p>
     * The list is structured such that for each variable in {@link #variableMetaModelList},
     * in order, it contains first the value of {@link #leftEntity} and then the value of {@link #rightEntity}.
     * Example: with two variables v1 and v2, the list contains [left.v1, right.v1, left.v2, right.v2].
     */
    private @Nullable List<@Nullable Object> valueCache;

    public SwapMove(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, Entity_ leftEntity,
            Entity_ rightEntity) {
        this.variableMetaModelList = Objects.requireNonNull(variableMetaModelList);
        if (variableMetaModelList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Swap move requires at least one planning variable to swap between entities, but got (%s)."
                            .formatted(variableMetaModelList));
        }
        this.leftEntity = Objects.requireNonNull(leftEntity);
        this.rightEntity = Objects.requireNonNull(rightEntity);
        if (leftEntity == rightEntity) {
            throw new IllegalArgumentException("Swap move requires two different entities (%s)."
                    .formatted(leftEntity));
        }
    }

    @Override
    public List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModels() {
        return variableMetaModelList;
    }

    public Entity_ getLeftEntity() {
        return leftEntity;
    }

    public Entity_ getRightEntity() {
        return rightEntity;
    }

    @Override
    public SwapMove<Solution_, Entity_> rebase(Rebaser rebaser) {
        return new SwapMove<>(variableMetaModelList, rebaser.rebase(leftEntity), rebaser.rebase(rightEntity));
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        var cachedValues = getCachedValues();
        for (var i = 0; i < cachedValues.size(); i += 2) {
            var variableMetaModel = variableMetaModelList.get(i / 2);
            var oldLeftValue = cachedValues.get(i);
            var oldRightValue = cachedValues.get(i + 1);
            if (Objects.equals(oldLeftValue, oldRightValue)
                    || !solutionView.isValueInRange(variableMetaModel, leftEntity, oldRightValue)
                    || !solutionView.isValueInRange(variableMetaModel, rightEntity, oldLeftValue)) {
                // No change needed, skip it.
                continue;
            }
            solutionView.changeVariable(variableMetaModel, leftEntity, oldRightValue);
            solutionView.changeVariable(variableMetaModel, rightEntity, oldLeftValue);
        }
    }

    private List<@Nullable Object> getCachedValues() {
        if (valueCache != null) {
            return valueCache;
        }
        valueCache = new ArrayList<>(variableMetaModelList.size() * 2);
        for (var variableMetaModel : variableMetaModelList) {
            var variableDescriptor = getVariableDescriptor(variableMetaModel);
            valueCache.add(variableDescriptor.getValue(leftEntity));
            valueCache.add(variableDescriptor.getValue(rightEntity));
        }
        return valueCache;
    }

    @Override
    public Collection<Entity_> extractPlanningEntities() {
        return List.of(leftEntity, rightEntity);
    }

    @Override
    public Collection<@Nullable Object> extractPlanningValues() {
        return new LinkedHashSet<>(getCachedValues()); // Not using Set.of(), as values may be null.
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SwapMove<?, ?> other
                && Objects.equals(variableMetaModelList, other.variableMetaModelList)
                && Objects.equals(leftEntity, other.leftEntity)
                && Objects.equals(rightEntity, other.rightEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModelList, leftEntity, rightEntity);
    }

    @Override
    public String toString() {
        var s = new StringBuilder(variableMetaModelList.size() * 16);
        s.append(leftEntity).append(" {");
        appendVariablesToString(s, true);
        s.append("} <-> ");
        s.append(rightEntity).append(" {");
        appendVariablesToString(s, false);
        s.append("}");
        return s.toString();
    }

    private void appendVariablesToString(StringBuilder s, boolean isLeftEntity) {
        var cachedValues = getCachedValues();
        for (var i = 0; i < cachedValues.size(); i += 2) {
            var index = isLeftEntity ? i : i + 1;
            var value = cachedValues.get(index);
            if (i > 0) {
                s.append(", ");
            }
            s.append(value == null ? "null" : value.toString());
        }
    }

}
