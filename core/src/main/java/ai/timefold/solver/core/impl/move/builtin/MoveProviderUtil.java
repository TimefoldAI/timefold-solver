package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.PillarChangeMoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDataset;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
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
     * Every entity currently assigned a non-null value, with no grouping -
     * unlike {@link #entitiesByAssignedValue}, members of one drawn sample need not share a value.
     */
    public static <Solution_, Entity_, Value_> UniDataset<Solution_, Entity_> assignedEntityDataset(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return assignedEntities(moveStreamFactory, variableMetaModel).asCachedDataset();
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
     * The movable entities which can be a swap partner of the given entity.
     * A valid partner either accepts the entity's value of a variable, or already has it;
     * either way, it is in that variable's list, so the smallest list over all variables is enough.
     * This assumes that every entity's value is in its own value range.
     * A partner whose shared value is outside its range is not found;
     * only an input solution can have such a value, and the swap would keep it.
     *
     * @return null if every variable of the entity is unassigned, so that no value narrows the candidates
     */
    public static <Solution_, Entity_> @Nullable List<Entity_> swapCandidatesOf(SolutionView<Solution_> solutionView,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, Entity_ entity) {
        return smallestCandidateList(solutionView, variableMetaModelList,
                index -> solutionView.getValue(variableMetaModelList.get(index), entity));
    }

    /**
     * As {@link #swapCandidatesOf(SolutionView, List, Object)},
     * but for the entities whose values are the given {@link #compositeKeyOf composite key}.
     */
    public static <Solution_, Entity_> @Nullable List<Entity_> swapCandidatesOfKey(SolutionView<Solution_> solutionView,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, List<Object> compositeKey) {
        return smallestCandidateList(solutionView, variableMetaModelList, compositeKey::get);
    }

    private static <Solution_, Entity_> @Nullable List<Entity_> smallestCandidateList(SolutionView<Solution_> solutionView,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList,
            IntFunction<@Nullable Object> valueByVariableIndex) {
        List<Entity_> candidateList = null;
        for (var index = 0; index < variableMetaModelList.size(); index++) {
            var variableMetaModel = variableMetaModelList.get(index);
            var value = valueByVariableIndex.apply(index);
            if (value == null) {
                continue;
            }
            var entityList = solutionView.getEntitiesWithValueInRange(variableMetaModel, value);
            if (candidateList == null || entityList.size() < candidateList.size()) {
                candidateList = entityList;
            }
        }
        return candidateList;
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
            // A solution value range accepts every value, except null if the variable does not allow it.
            if ((variableMetaModel.isValueRangeOnSolution() && oldLeftValue != null && oldRightValue != null)
                    || (leftRangesPerVariable.get(i).containsInEvery(oldRightValue)
                            && rightRangesPerVariable.get(i).containsInEvery(oldLeftValue))) {
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

    /**
     * @return {@code <provider class simple name>(<entity simple name>.<variable names>[,<setting>...])}, no spaces
     */
    public static String id(Class<?> providerClass, List<? extends VariableMetaModel<?, ?, ?>> variableMetaModelList,
            String... settings) {
        var id = new StringBuilder(providerClass.getSimpleName())
                .append('(')
                .append(variableMetaModelList.getFirst().entity().type().getSimpleName())
                .append('.')
                .append(variableMetaModelList.stream().map(VariableMetaModel::name).collect(Collectors.joining(",")));
        for (var setting : settings) {
            id.append(',').append(setting);
        }
        return id.append(')').toString();
    }

    public static String id(Class<?> providerClass, VariableMetaModel<?, ?, ?> variableMetaModel, String... settings) {
        return id(providerClass, List.of(variableMetaModel), settings);
    }

    /**
     * @return the name if true, else "no" + the capitalized name
     */
    public static String flag(String name, boolean value) {
        return value ? name : "no" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String range(int minimum, int maximum) {
        return minimum + "-" + maximum;
    }

    /**
     * @return the simple name of the class, or "custom" for a lambda or an anonymous class, whose names are not stable
     */
    public static String defaultId(Object instance) {
        var type = instance.getClass();
        return type.isHidden() || type.isAnonymousClass() || type.isSynthetic() ? "custom" : type.getSimpleName();
    }

    /**
     * The ID must be the same in every run with the same configuration:
     * move selection strategies give weights by ID, and the DEBUG log of IDs and weights is compared between runs.
     * The built-in samplers have a stable {@code toString()}.
     * A custom sampler may not, for example a lambda, whose default {@code toString()} contains an identity hash;
     * so every custom sampler is "custom".
     * The check uses the package of the class, which needs no reflection and so also works in a native image.
     *
     * @return the {@code toString()} of a built-in sampler, or "custom"
     */
    public static String samplerId(Object sampler) {
        return sampler.getClass().getPackageName().equals(Samplers.class.getPackageName()) ? sampler.toString()
                : "custom";
    }

    /**
     * @return true if the entity's value range contains every value
     */
    public static <Solution_, Entity_, Value_> boolean acceptsAll(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            Iterable<Value_> values) {
        var range = solutionView.getValueRange(variableMetaModel, entity);
        for (var value : values) {
            if (!range.contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if the entity's value range contains every value of the source entity
     *         from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
     */
    public static <Solution_, Entity_, Value_> boolean acceptsAll(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            Entity_ sourceEntity, int fromIndex, int toIndex) {
        var range = solutionView.getValueRange(variableMetaModel, entity);
        for (var index = fromIndex; index < toIndex; index++) {
            if (!range.contains(solutionView.getValueAtIndex(variableMetaModel, sourceEntity, index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * With a solution value range, keeps only the values in it, as the solution can hold other values of that class.
     * The filter runs once per change of a value, not on every draw; entity value ranges need no filter.
     */
    public static <Solution_, Entity_, Value_> UniEnumeratingStream<Solution_, Value_> inValueRange(
            MoveStreamFactory<Solution_> moveStreamFactory,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniEnumeratingStream<Solution_, Value_> valueStream) {
        if (!variableMetaModel.isValueRangeOnSolution()) {
            return valueStream;
        }
        return valueStream.filter(((DefaultMoveStreamFactory<Solution_>) moveStreamFactory)
                .getNodeSharingSupportFunctions(variableMetaModel).inSolutionValueRangeFilter());
    }
}
