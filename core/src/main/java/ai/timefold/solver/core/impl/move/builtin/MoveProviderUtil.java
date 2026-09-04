package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.MassChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.PillarChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MoveProviderUtil {

    /**
     * Every basic planning variable of the entity class, in natural order,
     * as defined by {@link PlanningVariableMetaModel#compareTo(Object)}.
     *
     * @param entityMetaModel
     * @return The list is guaranteed to be non-empty.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <Solution_, Entity_> List<PlanningVariableMetaModel<Solution_, Entity_, ?>>
            basicVariablesOf(GenuineEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        var variableMetaModelList = entityMetaModel.genuineVariables()
                .stream()
                .filter(v -> !v.isListVariable())
                .map(v -> (PlanningVariableMetaModel<Solution_, Entity_, ?>) v)
                .sorted()
                .toList();
        if (variableMetaModelList.isEmpty()) {
            throw new IllegalArgumentException("The entityClass (%s) has no basic planning variables."
                    .formatted(entityMetaModel.type().getCanonicalName()));
        }
        return (List) variableMetaModelList;
    }

    /**
     * Return these variables in their natural order, as defined by {@link PlanningVariableMetaModel#compareTo(Object)}.
     *
     *
     * @param variableMetaModelList Arbitrary list of variables.
     * @return Deduplicated, non-empty, sorted list of variables.
     * @throws IllegalArgumentException if the variableMetaModelList is empty,
     *         or if the variables come from multiple different entities.
     */
    @SuppressWarnings({ "rawtypes" })
    public static <Solution_, Entity_> List<PlanningVariableMetaModel<Solution_, Entity_, Object>>
            normalize(List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList) {
        var entityMetaModelList = Objects.requireNonNull(variableMetaModelList).stream()
                .map(PlanningVariableMetaModel::entity)
                .distinct()
                .toList();
        return switch (entityMetaModelList.size()) {
            case 0 -> throw new IllegalArgumentException("The variableMetaModelList (%s) is empty."
                    .formatted(variableMetaModelList));
            case 1 -> (List) variableMetaModelList.stream()
                    .distinct()
                    .sorted()
                    .toList();
            default -> throw new IllegalArgumentException(
                    "The variableMetaModelList (%s) contains variables from multiple entity classes."
                            .formatted(variableMetaModelList));
        };

    }

    /**
     * The entity's current value of every variable in {@code variableMetaModelList}, in that order.
     * Two entities with equal keys agree on every listed variable.
     *
     * @param entity
     * @param variableMetaModelList Assumed in a {@link #normalize(List) normalized} order.
     */
    public static <Solution_, Entity_> List<Object> compositeKeyOf(Entity_ entity,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList) {
        var size = variableMetaModelList.size();
        var values = new Object[size];
        for (var i = 0; i < size; i++) {
            var defaultVariableMetaModel =
                    (DefaultPlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModelList.get(i);
            values[i] = defaultVariableMetaModel.variableDescriptor().getValue(entity);
        }
        // Arrays.asList, not List.of, since a value (and therefore an array slot) may be null.
        return Arrays.asList(values);
    }

    public static <Solution_, Entity_, Value_> UniDataset<Solution_, Value_> distinctAssignedValues(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        // groupBy yields one element per group, which is exactly the set of occupied values.
        // No joiner can express "the distinct set of keys", so groupBy is required here.
        return assignedEntities(moveStreamFactory, variableMetaModel)
                .groupBy((solutionView, entity) -> solutionView.getValue(variableMetaModel, entity))
                .asCachedDataset();
    }

    public static <Solution_, Entity_, Value_> BiDataset<Solution_, Value_, Entity_> entitiesByAssignedValue(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var defaultVariableMetaModel = (DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
        var variableDescriptor = defaultVariableMetaModel.variableDescriptor();
        // A real equal-join: the joiner's plain Function reads the variable directly, bypassing SolutionView.
        // UniDataset.join(...) resolves to JustInTimeBiDataset, an indexed lookup,
        // so drawing a member of a slice costs O(1) to create rather than scanning every entity.
        return distinctAssignedValues(moveStreamFactory, variableMetaModel)
                .join(assignedEntities(moveStreamFactory, variableMetaModel),
                        NeighborhoodsJoiners.equal(Function.identity(), variableDescriptor::getValue));
    }

    /**
     * Every entity of the class, assigned or not.
     * Unlike {@link #assignedEntities}, this deliberately admits unassigned entities:
     * a {@code Mass*} sample drawn from it may contain them,
     * and the move built from that sample assigns them as a side effect, crossing null upward.
     * Used by {@link MassChangeMoveProvider} only when it is crossing null;
     * otherwise it uses {@link #assignedEntityDataset} instead.
     */
    public static <Solution_, Entity_, Value_> UniDataset<Solution_, Entity_> allEntities(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return moveStreamFactory.forEach(variableMetaModel.entity().type(), false).asCachedDataset();
    }

    /**
     * Every entity currently assigned a non-null value, with no grouping -
     * unlike {@link #entitiesByAssignedValue}, members of one drawn sample need not share a value.
     */
    public static <Solution_, Entity_, Value_> UniDataset<Solution_, Entity_> assignedEntityDataset(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return assignedEntities(moveStreamFactory, variableMetaModel).asCachedDataset();
    }

    /**
     * @return the value every member of {@code sample} currently holds,
     *         or {@code null} if any two members disagree,
     *         or if {@code sample} is entirely unassigned.
     *         Either answer is the correct {@code excludedValue} for {@link SampleValueRanges#findDestination}:
     *         {@code null} is never itself a candidate destination,
     *         so excluding "no shared value" excludes nothing.
     */
    public static <Solution_, Entity_, Value_> @Nullable Value_ sharedValueOf(Sample<Entity_> sample,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, SolutionView<Solution_> solutionView) {
        Value_ sharedValue = null;
        var first = true;
        for (var entity : sample) {
            var value = solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity));
            if (first) {
                sharedValue = value;
                first = false;
            } else if (!Objects.equals(sharedValue, value)) {
                return null;
            }
        }
        return sharedValue;
    }

    /**
     * @return {@code true} if at least one member of {@code sample} currently holds a non-null value;
     *         short-circuits on the first one. Used to keep a null destination from being offered for a sample
     *         that is already entirely unassigned, which would otherwise be a no-op move.
     */
    public static <Solution_, Entity_, Value_> boolean anyAssigned(Sample<Entity_> sample,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, SolutionView<Solution_> solutionView) {
        for (var entity : sample) {
            if (solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * One {@link SampleValueRanges} per variable in {@code variableMetaModelList}, in that order.
     */
    public static <Solution_, Entity_> List<SampleValueRanges<Object>> rangesPerVariableOf(Sample<Entity_> pillar,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            SolutionView<Solution_> solutionView) {
        var perVariableList = new ArrayList<SampleValueRanges<Object>>(variableMetaModelList.size());
        for (var variableMetaModel : variableMetaModelList) {
            perVariableList.add(SampleValueRanges.of(pillar, variableMetaModel, solutionView));
        }
        return perVariableList;
    }

    /**
     * Whether swapping {@code leftPillar} and {@code rightPillar} is legal across every variable in
     * {@code variableMetaModelList},
     * checked against each pillar's own {@link SampleValueRanges} ({@code leftRangesPerVariable},
     * {@code rightRangesPerVariable})
     * rather than an {@code isValueInRange} call per member.
     * Computing those ranges is the caller's job, not this method's:
     * whether a range is worth caching across calls depends on whether its pillar is stable across those calls,
     * which only the caller knows.
     *
     * @return {@code true} if the swap changes at least one variable,
     *         and every changed variable is legal on both sides;
     *         {@code false} if any variable's swap falls out of range for either side.
     *         {@code leftPillar} and {@code rightPillar} must both be homogeneous per every variable in
     *         {@code variableMetaModelList}:
     *         each pillar's {@link Sample#representative()} is read once per variable
     *         and stands in for every one of its members.
     */
    public static <Solution_, Entity_> boolean isValidSwap(SolutionView<Solution_> solutionView,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            Sample<Entity_> leftPillar, List<SampleValueRanges<Object>> leftRangesPerVariable, Sample<Entity_> rightPillar,
            List<SampleValueRanges<Object>> rightRangesPerVariable) {
        var change = false;
        var leftPillarRepresentative = Objects.requireNonNull(leftPillar.representative());
        var rightPillarRepresentative = Objects.requireNonNull(rightPillar.representative());
        for (var i = 0; i < variableMetaModelList.size(); i++) {
            var variableMetaModel = variableMetaModelList.get(i);
            var oldLeftValue = solutionView.getValue(variableMetaModel, leftPillarRepresentative);
            var oldRightValue = solutionView.getValue(variableMetaModel, rightPillarRepresentative);
            if (Objects.equals(oldLeftValue, oldRightValue)) {
                continue;
            }
            if (leftRangesPerVariable.get(i).containsInEvery(oldRightValue)
                    && rightRangesPerVariable.get(i).containsInEvery(oldLeftValue)) {
                change = true;
            } else {
                // One of the swaps falls out of range, skip this pair altogether.
                return false;
            }
        }
        return change;
    }

    /**
     * The values {@code leftEntity} and {@code rightEntity} would take after swapping every variable in
     * {@code variableMetaModelList},
     * one pair per variable in that order:
     * for variables v1 and v2, the result is [left.v1, right.v1, left.v2, right.v2].
     */
    public static <Solution_, Entity_> List<@Nullable Object> cachedValuesOf(Entity_ leftEntity, Entity_ rightEntity,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList) {
        var valueList = new ArrayList<@Nullable Object>(variableMetaModelList.size() * 2);
        for (var variableMetaModel : variableMetaModelList) {
            var defaultVariableMetaModel =
                    (DefaultPlanningVariableMetaModel<Solution_, Entity_, Object>) variableMetaModel;
            var variableDescriptor = defaultVariableMetaModel.variableDescriptor();
            valueList.add(variableDescriptor.getValue(leftEntity));
            valueList.add(variableDescriptor.getValue(rightEntity));
        }
        return valueList;
    }

    /**
     * Appends every other value of {@code cachedValues} (as produced by {@link #cachedValuesOf})
     * to {@code s}, comma-separated:
     * the left value of each pair if {@code left}, the right value otherwise.
     */
    public static void appendInterleavedRow(StringBuilder s, List<@Nullable Object> cachedValues, boolean left) {
        for (var i = 0; i < cachedValues.size(); i += 2) {
            var index = left ? i : i + 1;
            var value = cachedValues.get(index);
            if (i > 0) {
                s.append(", ");
            }
            s.append(value == null ? "null" : value.toString());
        }
    }

    /**
     * Package-visible so {@link PillarChangeMoveProvider} can build its own pipeline on top of it
     * (a settle-cached row bundling a pillar with its precomputed ranges).
     */
    public static <Solution_, Entity_, Value_> UniEnumeratingStream<Solution_, Entity_> assignedEntities(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        var entityStream = moveStreamFactory.forEach(variableMetaModel.entity().type(), false);
        // Filtering to assigned entities keeps null out of the group key:
        // a pillar is keyed on a shared value,
        // and unassigned is the absence of a value, not one.
        // This is why PillarChangeMoveProvider/SubPillarChangeMoveProvider can only ever cross null downward (unassign),
        // regardless of their own crossingNull flag.
        return entityStream.filter((solutionView, entity) -> solutionView.getValue(variableMetaModel, entity) != null);
    }

    private MoveProviderUtil() {
        // No external instances.
    }

}
