package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
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
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Swaps values of one or more {@link PlanningVariable}s between the members of two {@link Sample}s.
 * Requires to specify a (sub)set of variables to swap values of,
 * all of which must belong to the same entity class.
 * <p>
 * The caller MUST only provide pillars that do not share any members.
 * The constructor does not check this.
 * If the pillars overlap, the shared member is written twice - once for each pillar it belongs to -
 * and ends up holding whichever value was written last, losing its own original value with no exception.
 * {@code PillarSwapMoveProvider} and {@code SubPillarSwapMoveProvider} only ever build disjoint pillars.
 * <p>
 * Every member of a pillar is assumed to hold the same value for each of the listed variables
 * ({@link #getCachedValues()} reads the value off a single representative member, not off every member).
 * The caller MUST only pass homogeneous pillars.
 * A heterogeneous member either gets a no-op write,
 * or gets overwritten with the wrong value and loses its own value;
 * nothing detects either case.
 * {@code PillarSwapMoveProvider} and {@code SubPillarSwapMoveProvider} only ever build homogeneous pillars.
 * <p>
 * The caller MUST only provide pillars whose values can be swapped;
 * for example, if one of the values is not in the value range of a member of the other pillar,
 * swapping would lead to an invalid solution.
 * This move does not re-check that at execution time, matching {@link Moves#pillarSwap};
 * if the pair is invalid,
 * the move writes the out-of-range value anyway and the solution becomes invalid, with no exception.
 * The built-in providers never propose such a pair.
 * <p>
 * Similarly, a pair whose every listed variable already matches between the two pillars is accepted by the constructor and now
 * performs writes that produce no net change;
 * the built-in providers never propose such a pair either,
 * since they key pillars on the composite of all listed variables and only pair distinct keys.
 * <p>
 * The caller is responsible for ordering the given variables consistently;
 * this constructor does not reorder them.
 * Two moves over the same pillars and the same set of variables are only guaranteed to be equal
 * if the caller lists the variables in the same order both times.
 * {@code PillarSwapMoveProvider} and {@code SubPillarSwapMoveProvider} normalize the order for moves they build.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 */
@NullMarked
public final class PillarSwapMove<Solution_, Entity_> extends AbstractMove<Solution_> {

    private final List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList;
    private final Sample<Entity_> leftPillar;
    private final Sample<Entity_> rightPillar;

    /**
     * Cache of the values of the pillars' representative members at the time of the first call of {@link #getCachedValues()}.
     * Ideally, the method would first be called before the values are changed by the move,
     * so that the {@link #toString()} method shows the original values.
     * <p>
     * The list is structured such that for each variable in {@link #variableMetaModelList},
     * in order, it contains first the value of {@link #leftPillar} and then the value of {@link #rightPillar}.
     * Example: with two variables v1 and v2, the list contains [left.v1, right.v1, left.v2, right.v2].
     * <p>
     * Relies on pillar homogeneity: every member of a pillar is assumed to share the same value of a given variable,
     * so reading the head of the pillar is enough.
     */
    private @Nullable List<@Nullable Object> valueList;

    public PillarSwapMove(List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            Sample<Entity_> leftPillar, Sample<Entity_> rightPillar) {
        if (Objects.requireNonNull(variableMetaModelList).isEmpty()) {
            throw new IllegalArgumentException(
                    "Pillar swap move requires at least one planning variable to swap between pillars, but got (%s)."
                            .formatted(variableMetaModelList));
        }
        this.variableMetaModelList = variableMetaModelList;
        this.leftPillar = Objects.requireNonNull(leftPillar);
        this.rightPillar = Objects.requireNonNull(rightPillar);
    }

    @Override
    public List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModels() {
        return variableMetaModelList;
    }

    /**
     * @return the members of the pillar on the left side of the swap
     */
    public Sample<Entity_> getLeftPillar() {
        return leftPillar;
    }

    /**
     * @return the members of the pillar on the right side of the swap
     */
    public Sample<Entity_> getRightPillar() {
        return rightPillar;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        var cachedValues = getCachedValues(); // [left.v1, right.v1, left.v2, right.v2, ...]
        for (var i = 0; i < cachedValues.size(); i += 2) {
            var variableMetaModel = variableMetaModelList.get(i / 2);
            var oldLeftValue = cachedValues.get(i);
            var oldRightValue = cachedValues.get(i + 1);
            for (var entity : leftPillar) {
                solutionView.changeVariable(variableMetaModel, Objects.requireNonNull(entity), oldRightValue);
            }
            for (var entity : rightPillar) {
                solutionView.changeVariable(variableMetaModel, Objects.requireNonNull(entity), oldLeftValue);
            }
        }
    }

    private List<@Nullable Object> getCachedValues() {
        if (valueList != null) {
            return valueList;
        }
        var leftHead = Objects.requireNonNull(leftPillar.representative());
        var rightHead = Objects.requireNonNull(rightPillar.representative());
        valueList = MoveProviderUtil.cachedValuesOf(leftHead, rightHead, variableMetaModelList);
        return valueList;
    }

    @Override
    public PillarSwapMove<Solution_, Entity_> rebase(Lookup lookup) {
        return new PillarSwapMove<>(variableMetaModelList, leftPillar.rebase(lookup), rightPillar.rebase(lookup));
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        // Does not deduplicate; if the pillars overlap (a caller error, see the class javadoc),
        // the shared member is reported twice.
        var entityList = new ArrayList<>(leftPillar.size() + rightPillar.size());
        for (var entity : leftPillar) {
            entityList.add(entity);
        }
        for (var entity : rightPillar) {
            entityList.add(entity);
        }
        return entityList;
    }

    @Override
    public SequencedCollection<@Nullable Object> getPlanningValues() {
        return new LinkedHashSet<>(getCachedValues()); // Not using Set.of(), as values may be null.
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PillarSwapMove<?, ?> other
                && Objects.equals(variableMetaModelList, other.variableMetaModelList)
                && Objects.equals(leftPillar, other.leftPillar)
                && Objects.equals(rightPillar, other.rightPillar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModelList, leftPillar, rightPillar);
    }

    @Override
    public String toString() {
        var s = new StringBuilder(variableMetaModelList.size() * 16);
        var cachedValues = getCachedValues();
        s.append(leftPillar).append(" {");
        MoveProviderUtil.appendInterleavedRow(s, cachedValues, true);
        s.append("} <-> ");
        s.append(rightPillar).append(" {");
        MoveProviderUtil.appendInterleavedRow(s, cachedValues, false);
        s.append("}");
        return s.toString();
    }

}
