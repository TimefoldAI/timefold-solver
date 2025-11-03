package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

public abstract class AbstractFlattenLastNode<InTuple_ extends AbstractTuple, OutTuple_ extends AbstractTuple, EffectiveItem_, FlattenedItem_>
        extends AbstractNode
        implements TupleLifecycle<InTuple_> {

    private final int flattenLastStoreIndex;
    private final Function<EffectiveItem_, Iterable<FlattenedItem_>> mappingFunction;
    private final StaticPropagationQueue<OutTuple_> propagationQueue;

    protected AbstractFlattenLastNode(int flattenLastStoreIndex,
            Function<EffectiveItem_, Iterable<FlattenedItem_>> mappingFunction,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle) {
        this.flattenLastStoreIndex = flattenLastStoreIndex;
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    @Override
    public final void insert(InTuple_ tuple) {
        if (tuple.getStore(flattenLastStoreIndex) != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tuple
                    + ") was already added in the tupleStore.");
        }
        Iterable<FlattenedItem_> iterable = mappingFunction.apply(getEffectiveFactIn(tuple));
        if (iterable instanceof Collection<FlattenedItem_> collection) {
            // Optimization for Collection, where we know the size.
            int size = collection.size();
            if (size == 0) {
                return;
            }
            FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = new FlattenBagByItem<>(size);
            for (FlattenedItem_ item : collection) {
                addTuple(tuple, item, bagByItem);
            }
            tuple.setStore(flattenLastStoreIndex, bagByItem);
        } else {
            Iterator<FlattenedItem_> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return;
            }
            FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = new FlattenBagByItem<>();
            while (iterator.hasNext()) {
                addTuple(tuple, iterator.next(), bagByItem);
            }
            tuple.setStore(flattenLastStoreIndex, bagByItem);
        }
    }

    private void addTuple(InTuple_ originalTuple, FlattenedItem_ item,
            FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem) {
        var outTupleBag = bagByItem.getBag(item);
        outTupleBag.add(() -> createTuple(originalTuple, outTupleBag.value),
                propagationQueue::insert,
                propagationQueue::update);
    }

    protected abstract OutTuple_ createTuple(InTuple_ originalTuple, FlattenedItem_ item);

    @Override
    public final void update(InTuple_ tuple) {
        FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = tuple.getStore(flattenLastStoreIndex);
        if (bagByItem == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s).
            insert(tuple);
            return;
        }
        Iterable<FlattenedItem_> iterable = mappingFunction.apply(getEffectiveFactIn(tuple));
        for (var bag : bagByItem.getAllBags()) {
            bag.reset();
        }

        for (var item : iterable) {
            addTuple(tuple, item, bagByItem);
        }

        var bagIterator = bagByItem.getAllBags().iterator();
        while (bagIterator.hasNext()) {
            var bag = bagIterator.next();
            bag.removeExtras(this::removeTuple);
            if (bag.newCount.intValue() == 0) {
                bagIterator.remove();
            }
        }
    }

    protected abstract EffectiveItem_ getEffectiveFactIn(InTuple_ tuple);

    @Override
    public final void retract(InTuple_ tuple) {
        FlattenBagByItem<FlattenedItem_, OutTuple_> bagByItem = tuple.removeStore(flattenLastStoreIndex);
        if (bagByItem == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        for (var flattenLastEntry : bagByItem.getAllBags()) {
            flattenLastEntry.reset();
            flattenLastEntry.removeExtras(this::removeTuple);
        }
    }

    private void removeTuple(OutTuple_ outTuple) {
        TupleState state = outTuple.state;
        if (!state.isActive()) {
            throw new IllegalStateException("Impossible state: The tuple (" + outTuple +
                    ") is in an unexpected state (" + outTuple.state + ").");
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
            this(CollectionUtils.newLinkedHashMap(size));
        }

        /**
         * @return a {@link Collection} backed by {@link FlattenBagByItem}, so modifications
         *         to it are reflected in {@link FlattenBagByItem}.
         */
        Collection<FlattenItemBag<FlattenedItem_, OutTuple_>> getAllBags() {
            return delegate.values();
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

    private record FlattenItemBag<FlattenedItem_, OutTuple_>(FlattenedItem_ value, MutableInt newCount,
            List<OutTuple_> outTupleList) {
        FlattenItemBag(FlattenedItem_ value) {
            this(value, new MutableInt(), new ArrayList<>());
        }

        /**
         * Increments {@link #newCount}.
         * If the updated {@link #newCount} is less than or equal to the size of {@link #outTupleList},
         * the {@code updateConsumer} is called with the corresponding element from
         * {@link #outTupleList}.
         * Otherwise, the {@code insertConsumer} is called with a new tuple created
         * with {@code outTupleSupplier}, and that tuple is added to {@link #outTupleList}.
         */
        void add(Supplier<OutTuple_> outTupleSupplier,
                Consumer<OutTuple_> insertConsumer,
                Consumer<OutTuple_> updateConsumer) {
            var listIndex = newCount.intValue();
            newCount.increment();
            if (newCount.intValue() > outTupleList.size()) {
                var inserted = outTupleSupplier.get();
                outTupleList.add(inserted);
                insertConsumer.accept(inserted);
            } else {
                updateConsumer.accept(outTupleList.get(listIndex));
            }
        }

        /**
         * Calls {@code retractConsumer} on the tuples in {@link #outTupleList}
         * that are position at or after {@link #newCount}, and remove them
         * from the list (causing the size of the list to be {@link #newCount}).
         */
        void removeExtras(Consumer<OutTuple_> retractConsumer) {
            for (var i = newCount.intValue(); i < outTupleList.size(); i++) {
                retractConsumer.accept(outTupleList.get(i));
            }
            outTupleList.subList(newCount.intValue(), outTupleList.size()).clear();
        }

        /**
         * Sets {@link #newCount} to 0, while retaining the created tuples in {@link #outTupleList}.
         */
        void reset() {
            newCount.setValue(0);
        }
    }

}
