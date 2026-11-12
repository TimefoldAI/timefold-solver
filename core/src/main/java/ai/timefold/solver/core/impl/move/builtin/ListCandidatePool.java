package ai.timefold.solver.core.impl.move.builtin;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

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
 * whose value range contains the value ({@link SolutionView#getEntitiesWithValueInRange}),
 * padded to the longest of them, or through prefix sums of their positions if padding would be most of the slots
 * (see {@link SearchShape}); the shape is cached per value.
 * The single draws ({@link #drawDestinationOnce}, {@link #drawPartnerOnce}) are uniform over all movable entities,
 * so that they do not depend on the value.
 * Either way, every unpinned position has the same chance, and no pinned position is ever drawn.
 * The caller's filter checks everything else.
 * <p>
 * Create one instance per move iterator: the solution does not change while the iterator runs.
 */
@NullMarked
public final class ListCandidatePool<Solution_, Entity_, Value_>
        implements ListCandidateSource<Entity_, Value_> {

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param random the random generator of the move iterator
     * @param entityInstance the movable entities of the variable
     * @param includeUnassignedDestination whether the unassigned destination is part of the destinations,
     *        with the weight of one position
     */
    public static <Solution_, Entity_, Value_> ListCandidatePool<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance, boolean includeUnassignedDestination) {
        return new ListCandidatePool<>(solutionView, variableMetaModel, random, () -> toList(entityInstance, random),
                List::of, includeUnassignedDestination);
    }

    /**
     * As {@link #of(SolutionView, PlanningListVariableMetaModel, RandomGenerator, UniDatasetInstance, boolean)},
     * without the unassigned destination, and with unassigned values as swap partners.
     *
     * @param unassignedValueInstance the unassigned values, as swap partners
     */
    public static <Solution_, Entity_, Value_> ListCandidatePool<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance,
            UniDatasetInstance<Value_> unassignedValueInstance) {
        return new ListCandidatePool<>(solutionView, variableMetaModel, random, () -> toList(entityInstance, random),
                () -> toList(unassignedValueInstance, random), false);
    }

    private final SolutionView<Solution_> solutionView;
    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final RandomGenerator random;
    // Copied on first use: only the single draws read the entities, and only swaps read the unassigned values.
    private final Supplier<List<Entity_>> movableEntitySupplier;
    private final Supplier<List<Value_>> unassignedValueSupplier;
    private @Nullable List<Entity_> movableEntities;
    private @Nullable List<Value_> unassignedValues;
    private final boolean includeUnassignedDestination;
    // Per value: prefix sums of the position counts (destinations) and value counts (partners) of its accepting entities.
    private final Map<Value_, SearchShape> destinationShapeMap = new IdentityHashMap<>();
    private final Map<Value_, SearchShape> partnerShapeMap = new IdentityHashMap<>();
    // The same over all movable entities, for the single draws; built on first use.
    private long @Nullable [] cumulativePositionCounts;
    private long @Nullable [] cumulativeValueCounts;

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param random the random generator of the move iterator
     * @param movableEntitySupplier the movable entities of the variable, random access; called at most once
     * @param unassignedValueSupplier the unassigned values, for swap partners, random access; empty if none are acceptable;
     *        called at most once
     * @param includeUnassignedDestination whether the unassigned destination is part of the destinations
     */
    ListCandidatePool(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, RandomGenerator random,
            Supplier<List<Entity_>> movableEntitySupplier, Supplier<List<Value_>> unassignedValueSupplier,
            boolean includeUnassignedDestination) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.random = Objects.requireNonNull(random);
        this.movableEntitySupplier = Objects.requireNonNull(movableEntitySupplier);
        this.unassignedValueSupplier = Objects.requireNonNull(unassignedValueSupplier);
        this.includeUnassignedDestination = includeUnassignedDestination;
    }

    private List<Entity_> movableEntities() {
        if (movableEntities == null) {
            movableEntities = movableEntitySupplier.get();
        }
        return movableEntities;
    }

    private List<Value_> unassignedValues() {
        if (unassignedValues == null) {
            unassignedValues = unassignedValueSupplier.get();
        }
        return unassignedValues;
    }

    /**
     * Copies the dataset: its list can have gaps that make random access scan, and compacting it moves other slots.
     * Once datasets have no gaps during a step, this copy can become a view of the dataset's list.
     */
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

    /**
     * @return the destinations of the entity: one before each unpinned value, plus the end slot
     */
    private int positionCount(Entity_ entity) {
        return solutionView.countValues(variableMetaModel, entity)
                - solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + 1;
    }

    /**
     * @param entityList the entities to count over
     * @param excludedPerEntity 0 to count destinations, 1 to count values (the end slot holds no value)
     * @return the prefix sums of the counts, one per entity
     */
    private long[] cumulativeCounts(List<Entity_> entityList, int excludedPerEntity) {
        var result = new long[entityList.size()];
        long sum = 0;
        for (var i = 0; i < result.length; i++) {
            sum += positionCount(entityList.get(i)) - excludedPerEntity;
            result[i] = sum;
        }
        return result;
    }

    private static long total(long[] cumulativeCounts) {
        return cumulativeCounts.length == 0 ? 0 : cumulativeCounts[cumulativeCounts.length - 1];
    }

    /**
     * @param entityList the entities the prefix sums were computed over
     * @param cumulativeCounts the prefix sums from {@link #cumulativeCounts}
     * @param slot below the last prefix sum
     * @return the position of the slot, counted from the first unpinned index of its entity
     */
    private PositionInList cumulativePosition(List<Entity_> entityList, long[] cumulativeCounts, long slot) {
        // The first entity whose cumulative count is above the slot; entities without positions repeat a count.
        var index = 0;
        var high = cumulativeCounts.length - 1;
        while (index < high) {
            var middle = (index + high) >>> 1;
            if (cumulativeCounts[middle] > slot) {
                high = middle;
            } else {
                index = middle + 1;
            }
        }
        var entity = entityList.get(index);
        var offset = (int) (slot - (index == 0 ? 0 : cumulativeCounts[index - 1]));
        return ElementPosition.of(entity, solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + offset);
    }

    /**
     * Pads each accepting entity to the longest one and rejects the padding; prefix sums replace padding
     * when it would be more than half of the slots, so a failed search costs at most twice the real positions.
     *
     * @param maximumCount the most positions (or values) of any of the entities
     * @param cumulativeCounts null to pad; otherwise the prefix sums of the counts
     */
    private static final class SearchShape {

        private final int maximumCount;
        private final long @Nullable [] cumulativeCounts;

        private SearchShape(int maximumCount, long @Nullable [] cumulativeCounts) {
            this.maximumCount = maximumCount;
            this.cumulativeCounts = cumulativeCounts;
        }

        long slotCount(int entityCount) {
            return cumulativeCounts == null ? (long) entityCount * maximumCount : total(cumulativeCounts);
        }

    }

    /**
     * @param excludedPerEntity 0 for destinations, 1 for values (the end slot holds no value)
     */
    private SearchShape searchShape(List<Entity_> entityList, int excludedPerEntity) {
        var cumulativeCounts = cumulativeCounts(entityList, excludedPerEntity);
        var maximumCount = 0L;
        for (var i = 0; i < cumulativeCounts.length; i++) {
            maximumCount = Math.max(maximumCount, cumulativeCounts[i] - (i == 0 ? 0 : cumulativeCounts[i - 1]));
        }
        var paddedCount = cumulativeCounts.length * maximumCount;
        return new SearchShape((int) maximumCount, paddedCount > 2 * total(cumulativeCounts) ? cumulativeCounts : null);
    }

    /**
     * @param slot below {@link SearchShape#slotCount(int)}
     * @return null if the slot is padding beyond the entity's own positions
     */
    private @Nullable PositionInList searchPosition(List<Entity_> entityList, SearchShape shape, int excludedPerEntity,
            long slot) {
        var cumulativeCounts = shape.cumulativeCounts;
        if (cumulativeCounts != null) {
            return cumulativePosition(entityList, cumulativeCounts, slot);
        }
        var entity = entityList.get((int) (slot / shape.maximumCount));
        var offset = (int) (slot % shape.maximumCount);
        if (offset >= positionCount(entity) - excludedPerEntity) {
            return null;
        }
        return ElementPosition.of(entity, solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + offset);
    }

    @Override
    public @Nullable ElementPosition drawDestination(Value_ value, Predicate<ElementPosition> filter) {
        var entityList = solutionView.getEntitiesWithValueInRange(variableMetaModel, value);
        var shape = destinationShapeMap.computeIfAbsent(value, v -> searchShape(entityList, 0));
        var positionSlotCount = shape.slotCount(entityList.size());
        var slotCount = positionSlotCount + (includeUnassignedDestination ? 1 : 0);
        if (slotCount == 0) {
            return null;
        }
        return AbstractLeftDrivenMoveIterator.drawMatching(
                new DestinationIterator(slotCount, positionSlotCount, entityList, shape), slotCount,
                destination -> destination != null && filter.test(destination));
    }

    /**
     * As {@link #drawPartner(Object, boolean, Predicate)}, without unassigned partners,
     * so that every partner is in an entity which accepts the value.
     */
    @Override
    public @Nullable Value_ drawPartner(Value_ value, Predicate<Value_> filter) {
        return drawPartner(value, false, filter);
    }

    /**
     * Assigned partners come from the accepting entities, unassigned partners
     * from the smaller of {@link SolutionView#getValuesSharingRangeWith} and the unassigned values.
     *
     * @param value the value to find a partner for
     * @param includeUnassigned whether unassigned values are acceptable partners
     * @param filter the caller's validity check
     * @return null if no partner was accepted within the bail-out budget
     */
    public @Nullable Value_ drawPartner(Value_ value, boolean includeUnassigned, Predicate<Value_> filter) {
        var entityList = solutionView.getEntitiesWithValueInRange(variableMetaModel, value);
        List<Value_> unassignedCandidateList = List.of();
        var unassignedValueList = includeUnassigned ? unassignedValues() : List.<Value_> of();
        if (!unassignedValueList.isEmpty()) {
            // Both lists hold every unassigned value which shares a range with the value, each once;
            // the smaller one rejects fewer draws, and either keeps every valid partner at the same chance.
            var sharingList = solutionView.getValuesSharingRangeWith(variableMetaModel, value);
            unassignedCandidateList = sharingList.size() < unassignedValueList.size() ? sharingList : unassignedValueList;
        }
        var shape = partnerShapeMap.computeIfAbsent(value, v -> searchShape(entityList, 1));
        var valueSlotCount = shape.slotCount(entityList.size());
        var slotCount = valueSlotCount + unassignedCandidateList.size();
        if (slotCount == 0) {
            return null;
        }
        return AbstractLeftDrivenMoveIterator.drawMatching(
                new PartnerCandidateIterator(entityList, shape, valueSlotCount, unassignedCandidateList, slotCount),
                slotCount, partner -> partner != null && filter.test(partner));
    }

    /**
     * Makes one draw, uniform over the destinations of all movable entities.
     * The draw does not depend on the value, so that for several drawn values (spans, for example),
     * every valid pair of a value and a destination has the same chance.
     * The filter must check that the destination entity accepts the value.
     */
    @Override
    public @Nullable ElementPosition drawDestinationOnce(Predicate<ElementPosition> filter) {
        if (cumulativePositionCounts == null) {
            cumulativePositionCounts = cumulativeCounts(movableEntities(), 0);
        }
        var positionCount = total(cumulativePositionCounts);
        var slotCount = positionCount + (includeUnassignedDestination ? 1L : 0L);
        if (slotCount == 0) {
            return null;
        }
        var slot = random.nextLong(slotCount);
        var destination = slot < positionCount ? cumulativePosition(movableEntities(), cumulativePositionCounts, slot)
                : ElementPosition.unassigned();
        return filter.test(destination) ? destination : null;
    }

    /**
     * As {@link #drawDestinationOnce(Predicate)}, but uniform over the unpinned values of all movable entities.
     */
    @Override
    public @Nullable Value_ drawPartnerOnce(Predicate<Value_> filter) {
        if (cumulativeValueCounts == null) {
            cumulativeValueCounts = cumulativeCounts(movableEntities(), 1);
        }
        var valueCount = total(cumulativeValueCounts);
        if (valueCount == 0) {
            return null;
        }
        var position = cumulativePosition(movableEntities(), cumulativeValueCounts, random.nextLong(valueCount));
        var partner = solutionView.getValueAtIndex(variableMetaModel, position.entity(), position.index());
        return filter.test(partner) ? partner : null;
    }

    /**
     * @return the value with the fewest accepting entities; drawing for it rejects the fewest candidates
     */
    @Override
    public Value_ narrowestValue(Iterable<Value_> values) {
        return narrowestValue(solutionView, variableMetaModel, values);
    }

    @Override
    public Value_ narrowestValue(Entity_ entity, int fromIndex, int toIndex) {
        return narrowestValue(solutionView, variableMetaModel, new AbstractList<>() {

            @Override
            public Value_ get(int index) {
                return solutionView.getValueAtIndex(variableMetaModel, entity, fromIndex + index);
            }

            @Override
            public int size() {
                return toIndex - fromIndex;
            }

        });
    }

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param values at least one value
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

    private final class DestinationIterator
            implements Iterator<@Nullable ElementPosition> {

        private final long slotCount;
        private final long positionCount;
        private final List<Entity_> entityList;
        private final SearchShape shape;

        private DestinationIterator(long slotCount, long positionCount, List<Entity_> entityList, SearchShape shape) {
            this.slotCount = slotCount;
            this.positionCount = positionCount;
            this.entityList = entityList;
            this.shape = shape;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public @Nullable ElementPosition next() {
            var slot = random.nextLong(slotCount);
            return slot < positionCount ? searchPosition(entityList, shape, 0, slot)
                    : ElementPosition.unassigned();
        }
    }

    private final class PartnerCandidateIterator
            implements Iterator<@Nullable Value_> {

        private final List<Entity_> entityList;
        private final SearchShape shape;
        private final long valueCount;
        private final List<Value_> unassignedCandidateList;
        private final long slotCount;

        private PartnerCandidateIterator(List<Entity_> entityList, SearchShape shape, long valueCount,
                List<Value_> unassignedCandidateList, long slotCount) {
            this.entityList = entityList;
            this.shape = shape;
            this.valueCount = valueCount;
            this.unassignedCandidateList = unassignedCandidateList;
            this.slotCount = slotCount;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public @Nullable Value_ next() {
            var slot = random.nextLong(slotCount);
            if (slot < valueCount) {
                var position = searchPosition(entityList, shape, 1, slot);
                return position == null ? null
                        : solutionView.getValueAtIndex(variableMetaModel, position.entity(), position.index());
            }
            var partner = unassignedCandidateList.get((int) (slot - valueCount));
            // Assigned values are reached through their entity.
            return solutionView.isAssigned(variableMetaModel, partner) ? null : partner;
        }
    }

}
