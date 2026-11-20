package ai.timefold.solver.core.impl.move.builtin;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
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
 * Every draw picks a slot uniformly from a {@link SlotLayout}.
 * A slot is one destination (before an unpinned value, or the end slot) or one unpinned value of an entity;
 * see {@link SlotKind}.
 * The searching draws ({@link #drawDestination}, {@link #drawPartner}) use only the entities
 * whose value range contains the value ({@link SolutionView#getEntitiesWithValueInRange}); their layout is cached per value.
 * The single draws ({@link #drawDestinationOnce}, {@link #drawPartnerOnce}) use all movable entities,
 * so that they do not depend on the value.
 * Either way, every unpinned position has the same chance, and no pinned position is ever drawn.
 * The caller's filter checks everything else.
 * <p>
 * Create one instance per move iterator: the solution does not change while the iterator runs.
 */
@NullMarked
public final class ListCandidatePoolSource<Solution_, Entity_, Value_>
        implements ListCandidateSource<Entity_, Value_> {

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param random the random generator of the move iterator
     * @param entityInstance the movable entities of the variable
     * @param includeUnassignedDestination whether the unassigned destination is part of the destinations,
     *        with the weight of one position
     */
    public static <Solution_, Entity_, Value_> ListCandidatePoolSource<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance, boolean includeUnassignedDestination) {
        return new ListCandidatePoolSource<>(solutionView, variableMetaModel, random, orderedRows(entityInstance), null,
                includeUnassignedDestination);
    }

    /**
     * As {@link #of(SolutionView, PlanningListVariableMetaModel, RandomGenerator, UniDatasetInstance, boolean)},
     * without the unassigned destination, and with unassigned values as swap partners.
     *
     * @param unassignedValueInstance the unassigned values, as swap partners
     */
    public static <Solution_, Entity_, Value_> ListCandidatePoolSource<Solution_, Entity_, Value_> of(
            SolutionView<Solution_> solutionView, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            RandomGenerator random, UniDatasetInstance<Entity_> entityInstance,
            UniDatasetInstance<Value_> unassignedValueInstance) {
        return new ListCandidatePoolSource<>(solutionView, variableMetaModel, random, orderedRows(entityInstance),
                Objects.requireNonNull(unassignedValueInstance), false);
    }

    private final SolutionView<Solution_> solutionView;
    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final RandomGenerator random;
    // Read once, by the first single draw, while it builds its layout.
    private final Iterable<@Nullable Entity_> movableEntities;
    // Drawn from directly: a draw only needs one random unassigned value.
    private final @Nullable UniDatasetInstance<Value_> unassignedValueInstance;
    private @Nullable Iterator<@Nullable Value_> unassignedValueIterator;
    private final boolean includeUnassignedDestination;
    // Per value: the slots of its accepting entities.
    private final Map<Value_, SlotLayout> destinationLayoutMap = new IdentityHashMap<>();
    private final Map<Value_, SlotLayout> partnerLayoutMap = new IdentityHashMap<>();
    // The slots of all movable entities, for the single draws; built on first use.
    private @Nullable SlotLayout allDestinationLayout;
    private @Nullable SlotLayout allPartnerLayout;

    /**
     * @param solutionView the view of the working solution
     * @param variableMetaModel the list variable
     * @param random the random generator of the move iterator
     * @param movableEntities the movable entities of the variable; read at most twice, by the first single draw of each kind
     * @param unassignedValueInstance the unassigned values, for swap partners; null if none are acceptable
     * @param includeUnassignedDestination whether the unassigned destination is part of the destinations
     */
    ListCandidatePoolSource(SolutionView<Solution_> solutionView,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, RandomGenerator random,
            Iterable<@Nullable Entity_> movableEntities, @Nullable UniDatasetInstance<Value_> unassignedValueInstance,
            boolean includeUnassignedDestination) {
        this.solutionView = Objects.requireNonNull(solutionView);
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.random = Objects.requireNonNull(random);
        this.movableEntities = Objects.requireNonNull(movableEntities);
        this.unassignedValueInstance = unassignedValueInstance;
        this.includeUnassignedDestination = includeUnassignedDestination;
    }

    /**
     * Uniform over the unassigned values, with replacement; it skips the gaps of the dataset's list without copying it.
     */
    private Iterator<@Nullable Value_> unassignedValueIterator() {
        if (unassignedValueIterator == null) {
            unassignedValueIterator = Objects.requireNonNull(unassignedValueInstance).iterator(random);
        }
        return unassignedValueIterator;
    }

    /**
     * The rows in insertion order, without compacting the dataset (see {@link DefaultUniDatasetInstance#iterator()}).
     */
    @SuppressWarnings("unchecked")
    private static <Solution_, A> Iterable<@Nullable A> orderedRows(UniDatasetInstance<A> instance) {
        var defaultInstance = (DefaultUniDatasetInstance<Solution_, A>) instance;
        return defaultInstance::iterator;
    }

    private int slotCount(Entity_ entity, SlotKind kind) {
        var destinationCount = solutionView.countValues(variableMetaModel, entity)
                - solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + 1;
        return kind == SlotKind.DESTINATION ? destinationCount : destinationCount - 1;
    }

    @Override
    public @Nullable ElementPosition drawDestination(Value_ value, Predicate<ElementPosition> filter) {
        var layout = destinationLayoutMap.computeIfAbsent(value,
                v -> new SlotLayout(solutionView.getEntitiesWithValueInRange(variableMetaModel, v), SlotKind.DESTINATION,
                        true));
        var slotCount = layout.slotCount + (includeUnassignedDestination ? 1 : 0);
        if (slotCount == 0) {
            return null;
        }
        return AbstractLeftDrivenMoveIterator.drawMatching(new DestinationIterator(layout, slotCount), slotCount,
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
     * @param includeUnassigned whether unassigned values are acceptable partners;
     *        true only if this source was made with the unassigned values
     * @param filter the caller's validity check
     * @return null if no partner was accepted within the bail-out budget
     */
    public @Nullable Value_ drawPartner(Value_ value, boolean includeUnassigned, Predicate<Value_> filter) {
        // Null: the unassigned candidates are drawn from the unassigned values.
        List<Value_> unassignedCandidateList = null;
        var unassignedCandidateCount = 0;
        var unassignedValueCount = includeUnassigned ? Objects.requireNonNull(unassignedValueInstance).size() : 0;
        if (unassignedValueCount > 0) {
            // Both hold every unassigned value which shares a range with the value, each once;
            // the smaller one rejects fewer draws, and either keeps every valid partner at the same chance.
            var sharingList = solutionView.getValuesSharingRangeWith(variableMetaModel, value);
            if (sharingList.size() < unassignedValueCount) {
                unassignedCandidateList = sharingList;
                unassignedCandidateCount = sharingList.size();
            } else {
                unassignedCandidateCount = unassignedValueCount;
            }
        }
        var layout = partnerLayoutMap.computeIfAbsent(value,
                v -> new SlotLayout(solutionView.getEntitiesWithValueInRange(variableMetaModel, v), SlotKind.VALUE, true));
        var slotCount = layout.slotCount + unassignedCandidateCount;
        if (slotCount == 0) {
            return null;
        }
        return AbstractLeftDrivenMoveIterator.drawMatching(
                new PartnerIterator(layout, unassignedCandidateList, slotCount), slotCount,
                partner -> partner != null && filter.test(partner));
    }

    /**
     * Makes one draw, uniform over the destinations of all movable entities.
     * The draw does not depend on the value, so that for several drawn values (spans, for example),
     * every valid pair of a value and a destination has the same chance.
     * The filter must check that the destination entity accepts the value.
     */
    @Override
    public @Nullable ElementPosition drawDestinationOnce(Predicate<ElementPosition> filter) {
        if (allDestinationLayout == null) {
            allDestinationLayout = new SlotLayout(movableEntities, SlotKind.DESTINATION);
        }
        var slotCount = allDestinationLayout.slotCount + (includeUnassignedDestination ? 1L : 0L);
        if (slotCount == 0) {
            return null;
        }
        var slot = random.nextLong(slotCount);
        ElementPosition destination = slot < allDestinationLayout.slotCount
                ? allDestinationLayout.prefixPosition(slot)
                : ElementPosition.unassigned();
        return filter.test(destination) ? destination : null;
    }

    /**
     * As {@link #drawDestinationOnce(Predicate)}, but uniform over the unpinned values of all movable entities.
     */
    @Override
    public @Nullable Value_ drawPartnerOnce(Predicate<Value_> filter) {
        if (allPartnerLayout == null) {
            allPartnerLayout = new SlotLayout(movableEntities, SlotKind.VALUE);
        }
        if (allPartnerLayout.slotCount == 0) {
            return null;
        }
        var position = allPartnerLayout.prefixPosition(random.nextLong(allPartnerLayout.slotCount));
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

        private final SlotLayout layout;
        // The slots of the layout, plus the unassigned destination if included.
        private final long slotCount;

        private DestinationIterator(SlotLayout layout, long slotCount) {
            this.layout = layout;
            this.slotCount = slotCount;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public @Nullable ElementPosition next() {
            var slot = random.nextLong(slotCount);
            return slot < layout.slotCount ? layout.position(slot) : ElementPosition.unassigned();
        }
    }

    private final class PartnerIterator
            implements Iterator<@Nullable Value_> {

        private final SlotLayout layout;
        // Null: draw the unassigned candidates from the unassigned values.
        private final @Nullable List<Value_> unassignedCandidateList;
        // The slots of the layout, plus one per unassigned candidate.
        private final long slotCount;

        private PartnerIterator(SlotLayout layout, @Nullable List<Value_> unassignedCandidateList, long slotCount) {
            this.layout = layout;
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
            if (slot < layout.slotCount) {
                var position = layout.position(slot);
                return position == null ? null
                        : solutionView.getValueAtIndex(variableMetaModel, position.entity(), position.index());
            }
            // Each unassigned candidate has one slot; a draw from the unassigned values is uniform over them too.
            var partner = unassignedCandidateList != null ? unassignedCandidateList.get((int) (slot - layout.slotCount))
                    : unassignedValueIterator().next();
            // Assigned values are reached through their entity.
            return partner == null || solutionView.isAssigned(variableMetaModel, partner) ? null : partner;
        }
    }

    private enum SlotKind {
        /** One slot before each unpinned value, plus the end slot. */
        DESTINATION,
        /** One slot per unpinned value. */
        VALUE
    }

    /**
     * The slots of one kind over a list of entities, numbered in the order of the entities.
     * Either through prefix sums of the counts (no padding), or padded: each entity gets the same number of slots,
     * and a draw rejects the padding.
     */
    private final class SlotLayout {

        private final List<Entity_> entityList;
        private final SlotKind kind;
        private final long[] cumulativeCounts;
        private final boolean padded;
        private final int maximumCount;
        private final long slotCount;

        /**
         * @param mayPad whether to pad when padding is at most half of the slots,
         *        so that a failed search costs at most twice the real slots
         */
        private SlotLayout(List<Entity_> entityList, SlotKind kind, boolean mayPad) {
            this.entityList = entityList;
            this.kind = kind;
            var counts = new long[entityList.size()];
            var sum = 0L;
            var maximum = 0;
            for (var i = 0; i < counts.length; i++) {
                var count = slotCount(entityList.get(i), kind);
                maximum = Math.max(maximum, count);
                sum += count;
                counts[i] = sum;
            }
            var paddedCount = (long) counts.length * maximum;
            this.cumulativeCounts = counts;
            this.padded = mayPad && paddedCount <= 2 * sum;
            this.maximumCount = maximum;
            this.slotCount = padded ? paddedCount : sum;
        }

        /**
         * Without padding; reads the entities once, in order, and keeps them by position.
         *
         * @param entities null items are skipped
         */
        private SlotLayout(Iterable<@Nullable Entity_> entities, SlotKind kind) {
            var readEntityList = new ArrayList<Entity_>();
            var counts = new long[16];
            var sum = 0L;
            var maximum = 0;
            for (var entity : entities) {
                if (entity == null) {
                    continue;
                }
                var count = slotCount(entity, kind);
                maximum = Math.max(maximum, count);
                sum += count;
                if (readEntityList.size() == counts.length) {
                    counts = Arrays.copyOf(counts, counts.length * 2);
                }
                counts[readEntityList.size()] = sum;
                readEntityList.add(entity);
            }
            this.entityList = readEntityList;
            this.kind = kind;
            this.cumulativeCounts = Arrays.copyOf(counts, readEntityList.size());
            this.padded = false;
            this.maximumCount = maximum;
            this.slotCount = sum;
        }

        /**
         * @param slot below {@link #slotCount}
         * @return null if the slot is padding
         */
        private @Nullable PositionInList position(long slot) {
            return padded ? paddedPosition(slot) : prefixPosition(slot);
        }

        private @Nullable PositionInList paddedPosition(long slot) {
            // Rejects a padding slot instead of taking the next real one, so that every real slot has the same chance.
            var entity = entityList.get((int) (slot / maximumCount));
            var offset = (int) (slot % maximumCount);
            return offset < slotCount(entity, kind) ? positionAt(entity, offset) : null;
        }

        /**
         * @param slot below {@link #slotCount}
         */
        private PositionInList prefixPosition(long slot) {
            // The first entity whose cumulative count is above the slot; entities without slots repeat a count.
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
            return positionAt(entityList.get(index), (int) (slot - (index == 0 ? 0 : cumulativeCounts[index - 1])));
        }

        private PositionInList positionAt(Entity_ entity, int offset) {
            return ElementPosition.of(entity, solutionView.getFirstUnpinnedIndex(variableMetaModel, entity) + offset);
        }

    }

}
