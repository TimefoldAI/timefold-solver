package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.LinkedHashSet;
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
 * Swaps values of {@link PlanningVariable} between two different {@link PlanningEntity} instances.
 * Requires to specify a (sub)set of variables to swap values of,
 * all of which must belong to the same entity class.
 *
 * <p>
 * The caller MUST only provide entities whose values can be swapped;
 * for example, if one of the values is not in the value range of the other entity's variable,
 * swapping would lead to an invalid solution.
 * This move does not re-check that at execution time, matching {@link Moves#swap};
 * if the pair is invalid, the move writes the out-of-range value anyway and the solution becomes invalid,
 * with no exception.
 * The built-in {@code SwapMoveProvider} never proposes such a pair.
 * <p>
 * Similarly, a move over two entities that already hold equal values on every listed variable now performs writes that produce
 * no net change;
 * the built-in {@code SwapMoveProvider} never proposes such a move either.
 * <p>
 * The caller is responsible for ordering the given variables consistently;
 * this constructor does not reorder them.
 * Two moves over the same entities and the same set of variables are only guaranteed to be equal
 * if the caller lists the variables in the same order both times.
 * {@code SwapMoveProvider} normalizes the order for moves it builds.
 *
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

    SwapMove(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, Entity_ leftEntity,
            Entity_ rightEntity) {
        if (variableMetaModelList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Swap move requires at least one planning variable to swap between entities, but got (%s)."
                            .formatted(variableMetaModelList));
        }
        this.variableMetaModelList = variableMetaModelList;
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
    public SwapMove<Solution_, Entity_> rebase(Lookup lookup) {
        return new SwapMove<>(variableMetaModelList, lookup.lookUpNonNullWorkingObject(leftEntity),
                lookup.lookUpNonNullWorkingObject(rightEntity));
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        var cachedValues = getCachedValues();
        for (var i = 0; i < cachedValues.size(); i += 2) {
            var variableMetaModel = variableMetaModelList.get(i / 2);
            var oldLeftValue = cachedValues.get(i);
            var oldRightValue = cachedValues.get(i + 1);
            solutionView.changeVariable(variableMetaModel, leftEntity, oldRightValue);
            solutionView.changeVariable(variableMetaModel, rightEntity, oldLeftValue);
        }
    }

    private List<@Nullable Object> getCachedValues() {
        if (valueCache == null) {
            valueCache = MoveProviderUtil.cachedValuesOf(leftEntity, rightEntity, variableMetaModelList);
        }
        return valueCache;
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        return List.of(leftEntity, rightEntity);
    }

    @Override
    public SequencedCollection<@Nullable Object> getPlanningValues() {
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
        var cachedValues = getCachedValues();
        s.append(leftEntity).append(" {");
        MoveProviderUtil.appendInterleavedRow(s, cachedValues, true);
        s.append("} <-> ");
        s.append(rightEntity).append(" {");
        MoveProviderUtil.appendInterleavedRow(s, cachedValues, false);
        s.append("}");
        return s.toString();
    }

}
