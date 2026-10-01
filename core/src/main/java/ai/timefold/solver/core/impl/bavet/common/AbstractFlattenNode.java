package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;

import org.jspecify.annotations.Nullable;

public abstract class AbstractFlattenNode<InTuple_ extends Tuple, OutTuple_ extends Tuple, FlattenedItem_>
        extends AbstractSingleInputNode<InTuple_> {

    private final int flattenStoreIndex;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;
    private final Consumer<OutTuple_> removeTupleConsumer = this::removeTuple;
    private final Predicate<FlattenItemBag<FlattenedItem_, OutTuple_>> removeExtrasPredicate =
            bag -> bag.removeExtras(removeTupleConsumer);

    protected AbstractFlattenNode(int flattenStoreIndex, TupleLifecycle<OutTuple_> nextNodesTupleLifecycle) {
        super(nextNodesTupleLifecycle);
        this.flattenStoreIndex = flattenStoreIndex;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    @Override
    public StreamKind getStreamKind() {
        return StreamKind.FLATTEN;
    }

    @Override
    public final void insert(InTuple_ tuple) {
        if (tuple.getStore(flattenStoreIndex) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(tuple));
        }
        var iterable = extractIterable(tuple);
        if (iterable instanceof Collection<FlattenedItem_> collection) {
            // Optimization for Collection, where we know the size.
            var size = collection.size();
            if (size == 0) {
                return;
            }
            var bagByItem = new FlattenBagByItem<FlattenedItem_, OutTuple_>(size);
            for (var item : collection) {
                addTuple(tuple, item, bagByItem);
            }
            tuple.setStore(flattenStoreIndex, bagByItem);
        } else {
            var iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return;
            }
            var bagByItem = new FlattenBagByItem<FlattenedItem_, OutTuple_>();
            while (iterator.hasNext()) {
                addTuple(tuple, iterator.next(), bagByItem);
            }
            tuple.setStore(flattenStoreIndex, bagByItem);
        }
    }

    private void addTuple(InTuple_ originalTuple, FlattenedItem_ item,
            FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem) {
        var bag = bagByItem.getBag(item);
        var reuse = bag.reuseOrAdvance();
        if (reuse == null) {
            var created = createTuple(originalTuple, bag.value);
            bag.append(created);
            propagationQueue.insert(created);
        } else {
            propagationQueue.update(reuse);
        }
    }

    protected abstract OutTuple_ createTuple(InTuple_ originalTuple, FlattenedItem_ item);

    @Override
    public final void update(InTuple_ tuple) {
        FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = tuple.getStore(flattenStoreIndex);
        if (bagByItem == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s).
            insert(tuple);
            return;
        }

        bagByItem.resetAll();
        for (var item : extractIterable(tuple)) {
            addTuple(tuple, item, bagByItem);
        }
        bagByItem.getAllBags()
                .removeIf(removeExtrasPredicate);
    }

    protected abstract Iterable<FlattenedItem_> extractIterable(InTuple_ tuple);

    @Override
    public final void retract(InTuple_ tuple) {
        FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = tuple.removeStore(flattenStoreIndex);
        if (bagByItem == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        bagByItem.applyToAll(removeTupleConsumer);
    }

    private void removeTuple(OutTuple_ outTuple) {
        var state = outTuple.getState();
        if (!state.isActive()) {
            throw new IllegalStateException("Impossible state: The tuple (%s) is in an unexpected state (%s)."
                    .formatted(outTuple, state));
        }
        propagationQueue.retract(outTuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    private record FlattenBagByItem<FlattenedItem_, OutTuple_>(
            Map<FlattenedItem_, FlattenItemBag<FlattenedItem_, OutTuple_>> delegate) {

        FlattenBagByItem() {
            this(new LinkedHashMap<>());
        }

        FlattenBagByItem(int size) {

            this(LinkedHashMap.newLinkedHashMap(size));
        }

        /**
         * @return a {@link Collection} backed by {@link FlattenBagByItem}, so modifications
         *         to it are reflected in {@link FlattenBagByItem}.
         */
        Collection<FlattenItemBag<FlattenedItem_, OutTuple_>> getAllBags() {
            return delegate.values();
        }

        void applyToAll(Consumer<OutTuple_> retractConsumer) {
            delegate.forEach((key, value) -> value.clear(retractConsumer));
        }

        void resetAll() {
            delegate.forEach((key, value) -> value.reset());
        }

        /**
         * @param key the item to get the bag of
         * @return the {@link FlattenItemBag} containing {@code key}, creating a new
         *         {@link FlattenItemBag} if it does not exist.
         */
        FlattenItemBag<FlattenedItem_, OutTuple_> getBag(FlattenedItem_ key) {
            return delegate.computeIfAbsent(key, FlattenItemBag::new);
        }
    }

    private static final class FlattenItemBag<FlattenedItem_, OutTuple_> {

        private final FlattenedItem_ value;
        private final List<OutTuple_> outTupleList = new ArrayList<>();
        private int newCount = 0;

        FlattenItemBag(FlattenedItem_ value) {
            this.value = value;
        }

        /**
         * Increments {@link #newCount}.
         *
         * @return the existing tuple to reuse if {@link #newCount} is within the current size of
         *         {@link #outTupleList}, or {@code null} if a new tuple must be created and passed
         *         to {@link #append}.
         */
        @Nullable
        OutTuple_ reuseOrAdvance() {
            var listIndex = newCount++;
            return newCount > outTupleList.size() ? null : outTupleList.get(listIndex);
        }

        void append(OutTuple_ created) {
            outTupleList.add(created);
        }

        /**
         * Calls {@code retractConsumer} on the tuples in {@link #outTupleList}
         * that are position at or after {@link #newCount}, and remove them
         * from the list (causing the size of the list to be {@link #newCount}).
         *
         * @return true if after removal, {@link #newCount} is 0
         */
        boolean removeExtras(Consumer<OutTuple_> retractConsumer) {
            var size = outTupleList.size();
            for (var i = size - 1; i >= newCount; i--) {
                // We go backwards to only shift the minimal amount of elements.
                // Also, it makes the loop simpler, because elements that need to be removed do not change position.
                retractConsumer.accept(outTupleList.remove(i));
            }
            return newCount == 0;
        }

        /**
         * Sets {@link #newCount} to 0, while retaining the created tuples in {@link #outTupleList}.
         */
        void reset() {
            newCount = 0;
        }

        void clear(Consumer<OutTuple_> retractConsumer) {
            outTupleList.forEach(retractConsumer);
            outTupleList.clear();
            newCount = 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FlattenItemBag<?, ?> other &&
                    this.newCount == other.newCount &&
                    Objects.equals(this.value, other.value) &&
                    Objects.equals(this.outTupleList, other.outTupleList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(newCount, value, outTupleList);
        }

        @Override
        public String toString() {
            return "FlattenItemBag[" +
                    "value=" + value + ", " +
                    "newCount=" + newCount + ", " +
                    "outTupleList=" + outTupleList + ']';
        }

    }

}
