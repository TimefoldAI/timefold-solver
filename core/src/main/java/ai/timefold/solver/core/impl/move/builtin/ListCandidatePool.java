package ai.timefold.solver.core.impl.move.builtin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Draws destinations or swap partners for a value of a list variable with entity value ranges.
 * <p>
 * The searching draws ({@link #drawDestination}, {@link #drawPartner}) use only the entities
 * whose value range contains the value ({@link SolutionView#getEntitiesWithValueInRange}):
 * a draw picks one of them and an offset below the largest number of positions among them;
 * an offset beyond the entity's own positions is rejected.
 * The single draws ({@link #drawDestinationOnce}, {@link #drawPartnerOnce}) are uniform over all movable entities,
 * so that they do not depend on the value.
 * Either way, every unpinned position has the same chance, and no pinned position is ever drawn.
 * The caller's filter checks everything else.
 * <p>
 * Create one instance per move iterator: the solution does not change while the iterator runs.
 */
@NullMarked
public final class ListCandidatePool<Solution_, Entity_, Value_> {

    public static <Solution_, Entity_, Value_> ListCandidatePool<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance) {
        return new ListCandidatePool<>(solutionView, variableMetaModel, random, toList(entityInstance, random), List.of());
    }

    /**
     * @param unassignedValueInstance the unassigned values, as swap partners
     */
    public static <Solution_, Entity_, Value_> ListCandidatePool<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance,
            UniDatasetInstance<Value_> unassignedValueInstance) {
        return new ListCandidatePool<>(solutionView, variableMetaModel, random, toList(entityInstance, random),
                toList(unassignedValueInstance, random));
    }

    private final SolutionView<Solution_> solutionView;
    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final RandomGenerator random;
    private final List<Entity_> movableEntities;
    private final List<Value_> unassignedValues;
    // The destinations of an entity: one before each unpinned value, plus the end slot.
    private final int[] positionCounts;
    // Per value: the largest position count among its accepting entities, the padding of the searching draws.
    private final Map<Value_, Integer> maximumPositionCountMap = new IdentityHashMap<>();
    // Prefix sums over the movable entities, for the single draws; built on first use.
    private long @Nullable [] cumulativePositionCounts;
    private long @Nullable [] cumulativeValueCounts;

    /**
     * @param movableEntities the movable entities of the variable
     * @param unassignedValues the unassigned values, for swap partners; empty if none are acceptable
     */
    ListCandidatePool(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, RandomGenerator random,
            List<Entity_> movableEntities, List<Value_> unassignedValues) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.random = Objects.requireNonNull(random);
        this.movableEntities = Objects.requireNonNull(movableEntities);
        this.unassignedValues = Objects.requireNonNull(unassignedValues);
        this.positionCounts = new int[movableEntities.size()];
        for (var i = 0; i < positionCounts.length; i++) {
            positionCounts[i] = positionCount(movableEntities.get(i));
        }
    }

    private static <A> List<A> toList(UniDatasetInstance<A> instance, RandomGenerator random) {
        var list = new ArrayList<A>(instance.size());
        // Not a plain iterator: it compacts the dataset, which moves the slots that other move iterators hold.
        var iterator = instance.exhaustiveIterator(random);
        while (iterator.hasNext()) {
            var item = iterator.next();
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    private int positionCount(Entity_ entity) {
        return solutionView.countValues(variableMetaModel, entity)
                - solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + 1;
    }

    private int maximumPositionCount(Value_ value, List<Entity_> entityList) {
        return maximumPositionCountMap.computeIfAbsent(value, v -> {
            var maximum = 0;
            for (var entity : entityList) {
                maximum = Math.max(maximum, positionCount(entity));
            }
            return maximum;
        });
    }

    /**
     * Maps a slot of the padded pool of the given entities to a position.
     *
     * @param valuesOnly whether the end slot, which holds no value, is excluded
     * @return null if the slot is beyond the entity's own positions
     */
    private @Nullable PositionInList paddedPosition(List<Entity_> entityList, int maximumPositionCount, long slot,
            boolean valuesOnly) {
        var entity = entityList.get((int) (slot / maximumPositionCount));
        var offset = (int) (slot % maximumPositionCount);
        if (offset >= positionCount(entity) - (valuesOnly ? 1 : 0)) {
            return null;
        }
        return ElementPosition.of(entity, solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + offset);
    }

    /**
     * @param includeUnassigned whether the unassigned destination is part of the pool, with the weight of one position
     * @param filter the caller's validity check
     * @return null if no destination was accepted within the bail-out budget
     */
    public @Nullable ElementPosition drawDestination(Value_ value, boolean includeUnassigned,
            Predicate<ElementPosition> filter) {
        var entityList = solutionView.getEntitiesWithValueInRange(variableMetaModel, value);
        var maximumPositionCount = maximumPositionCount(value, entityList);
        var positionSlotCount = (long) entityList.size() * maximumPositionCount;
        var slotCount = positionSlotCount + (includeUnassigned ? 1 : 0);
        if (slotCount == 0) {
            return null;
        }
        return AbstractLeftDrivenMoveIterator.drawMatching(Stream.generate(() -> {
            var slot = random.nextLong(slotCount);
            return slot < positionSlotCount ? paddedPosition(entityList, maximumPositionCount, slot, false)
                    : ElementPosition.unassigned();
        }).iterator(), slotCount, destination -> destination != null && filter.test(destination));
    }

    /**
     * Assigned partners come from the accepting entities, unassigned partners
     * from the smaller of {@link SolutionView#getValuesSharingRangeWith} and the unassigned values.
     *
     * @param includeUnassigned whether unassigned values are acceptable partners
     * @param filter the caller's validity check
     * @return null if no partner was accepted within the bail-out budget
     */
    public @Nullable Value_ drawPartner(Value_ value, boolean includeUnassigned, Predicate<Value_> filter) {
        var entityList = solutionView.getEntitiesWithValueInRange(variableMetaModel, value);
        List<Value_> unassignedCandidateList = List.of();
        if (includeUnassigned) {
            var sharingList = solutionView.getValuesSharingRangeWith(variableMetaModel, value);
            unassignedCandidateList = sharingList.size() < unassignedValues.size() ? sharingList : unassignedValues;
        }
        var maximumPositionCount = maximumPositionCount(value, entityList);
        var positionSlotCount = (long) entityList.size() * maximumPositionCount;
        var slotCount = positionSlotCount + unassignedCandidateList.size();
        if (slotCount == 0) {
            return null;
        }
        var finalUnassignedCandidateList = unassignedCandidateList;
        return AbstractLeftDrivenMoveIterator.drawMatching(Stream.<@Nullable Value_> generate(() -> {
            var slot = random.nextLong(slotCount);
            if (slot < positionSlotCount) {
                var position = paddedPosition(entityList, maximumPositionCount, slot, true);
                return position == null ? null
                        : solutionView.getValueAtIndex(variableMetaModel, position.entity(), position.index());
            }
            var partner = finalUnassignedCandidateList.get((int) (slot - positionSlotCount));
            // Assigned values are reached through their entity.
            return solutionView.isAssigned(variableMetaModel, partner) ? null : partner;
        }).iterator(), slotCount, partner -> partner != null && filter.test(partner));
    }

    /**
     * Makes one draw, uniform over the destinations of all movable entities.
     * The draw does not depend on the value, so that for several drawn values (spans, for example),
     * every valid pair of a value and a destination has the same chance.
     * The filter must check that the destination entity accepts the value.
     *
     * @param includeUnassigned whether the unassigned destination is part of the pool, with the weight of one position
     * @param filter the caller's validity check
     * @return null if the draw did not give a valid destination
     */
    public @Nullable ElementPosition drawDestinationOnce(boolean includeUnassigned, Predicate<ElementPosition> filter) {
        var cumulativeCounts = cumulativePositionCounts();
        var positionCount = cumulativeCounts.length == 0 ? 0 : cumulativeCounts[cumulativeCounts.length - 1];
        var slotCount = positionCount + (includeUnassigned ? 1L : 0L);
        if (slotCount == 0) {
            return null;
        }
        var slot = random.nextLong(slotCount);
        var destination = slot < positionCount ? cumulativePosition(cumulativeCounts, slot) : ElementPosition.unassigned();
        return filter.test(destination) ? destination : null;
    }

    /**
     * As {@link #drawDestinationOnce(boolean, Predicate)}, but uniform over the unpinned values of all movable entities.
     *
     * @return null if the draw did not give a valid partner
     */
    public @Nullable Value_ drawPartnerOnce(Predicate<Value_> filter) {
        var cumulativeCounts = cumulativeValueCounts();
        if (cumulativeCounts.length == 0 || cumulativeCounts[cumulativeCounts.length - 1] == 0) {
            return null;
        }
        var position = cumulativePosition(cumulativeCounts, random.nextLong(cumulativeCounts[cumulativeCounts.length - 1]));
        var partner = solutionView.getValueAtIndex(variableMetaModel, position.entity(), position.index());
        return filter.test(partner) ? partner : null;
    }

    private long[] cumulativePositionCounts() {
        if (cumulativePositionCounts == null) {
            cumulativePositionCounts = cumulativeCounts(0);
        }
        return cumulativePositionCounts;
    }

    private long[] cumulativeValueCounts() {
        if (cumulativeValueCounts == null) {
            cumulativeValueCounts = cumulativeCounts(1);
        }
        return cumulativeValueCounts;
    }

    // The end slot is the last position of an entity; excluding it leaves the values.
    private long[] cumulativeCounts(int excludedPerEntity) {
        var result = new long[positionCounts.length];
        long sum = 0;
        for (var i = 0; i < positionCounts.length; i++) {
            sum += positionCounts[i] - excludedPerEntity;
            result[i] = sum;
        }
        return result;
    }

    private PositionInList cumulativePosition(long[] cumulativeCounts, long slot) {
        // The first entity whose cumulative count is above the slot; entities without positions repeat a count.
        var index = Arrays.binarySearch(cumulativeCounts, slot + 1);
        if (index < 0) {
            index = -index - 1;
        } else {
            while (index > 0 && cumulativeCounts[index - 1] == slot + 1) {
                index--;
            }
        }
        var entity = movableEntities.get(index);
        var offset = (int) (slot - (index == 0 ? 0 : cumulativeCounts[index - 1]));
        return ElementPosition.of(entity, solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + offset);
    }

    /**
     * As {@link #narrowestValue(SolutionView, PlanningListVariableMetaModel, Iterable)},
     * for the values of the entity from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     */
    public Value_ narrowestValue(Entity_ entity, int fromIndex, int toIndex) {
        Iterable<Value_> values = () -> IntStream.range(fromIndex, toIndex)
                .mapToObj(index -> solutionView.getValueAtIndex(variableMetaModel, entity, index))
                .iterator();
        return narrowestValue(solutionView, variableMetaModel, values);
    }

    /**
     * @return the value with the fewest accepting entities; drawing for it rejects the fewest candidates
     */
    public static <Solution_, Entity_, Value_> Value_ narrowestValue(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Iterable<Value_> values) {
        Value_ narrowestValue = null;
        var narrowestSize = Integer.MAX_VALUE;
        for (var value : values) {
            var size = solutionView.getEntitiesWithValueInRange(variableMetaModel, value).size();
            if (size < narrowestSize) {
                narrowestValue = value;
                narrowestSize = size;
                if (size == 0) { // Nothing can be narrower.
                    break;
                }
            }
        }
        return Objects.requireNonNull(narrowestValue);
    }

}
