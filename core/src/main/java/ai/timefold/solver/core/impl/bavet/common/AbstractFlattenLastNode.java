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
            Map<Object, FlattenLastMapEntry<FlattenedItem_, OutTuple_>> outMap = CollectionUtils.newLinkedHashMap(size);
            for (FlattenedItem_ item : collection) {
                addTuple(tuple, item, outMap);
            }
            tuple.setStore(flattenLastStoreIndex, outMap);
        } else {
            Iterator<FlattenedItem_> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Map<Object, FlattenLastMapEntry<FlattenedItem_, OutTuple_>> outMap = new LinkedHashMap<>();
            while (iterator.hasNext()) {
                addTuple(tuple, iterator.next(), outMap);
            }
            tuple.setStore(flattenLastStoreIndex, outMap);
        }
    }

    private void addTuple(InTuple_ originalTuple, FlattenedItem_ item,
            Map<Object, FlattenLastMapEntry<FlattenedItem_, OutTuple_>> outTupleMap) {
        var outTupleEntry = outTupleMap.computeIfAbsent(item, k -> new FlattenLastMapEntry<>(item));
        outTupleEntry.add(() -> createTuple(originalTuple, outTupleEntry.value),
                propagationQueue::insert,
                propagationQueue::update);
    }

    protected abstract OutTuple_ createTuple(InTuple_ originalTuple, FlattenedItem_ item);

    @Override
    public final void update(InTuple_ tuple) {
        Map<Object, FlattenLastMapEntry<FlattenedItem_, OutTuple_>> outTupleMap = tuple.getStore(flattenLastStoreIndex);
        if (outTupleMap == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s).
            insert(tuple);
            return;
        }
        Iterable<FlattenedItem_> iterable = mappingFunction.apply(getEffectiveFactIn(tuple));
        for (var flattenLastEntry : outTupleMap.values()) {
            flattenLastEntry.reset();
        }

        for (var item : iterable) {
            addTuple(tuple, item, outTupleMap);
        }

        var flattenLastEntryIterator = outTupleMap.values().iterator();
        while (flattenLastEntryIterator.hasNext()) {
            var next = flattenLastEntryIterator.next();
            next.retract(this::removeTuple);
            if (next.newCount.intValue() == 0) {
                flattenLastEntryIterator.remove();
            }
        }
    }

    protected abstract EffectiveItem_ getEffectiveFactIn(InTuple_ tuple);

    @Override
    public final void retract(InTuple_ tuple) {
        Map<Object, FlattenLastMapEntry<FlattenedItem_, OutTuple_>> outTupleMap = tuple.removeStore(flattenLastStoreIndex);
        if (outTupleMap == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        for (FlattenLastMapEntry<FlattenedItem_, OutTuple_> flattenLastEntry : outTupleMap.values()) {
            flattenLastEntry.reset();
            flattenLastEntry.retract(this::removeTuple);
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

    private record FlattenLastMapEntry<FlattenedItem_, OutTuple_>(FlattenedItem_ value, MutableInt newCount,
            List<OutTuple_> outTupleList) {
        FlattenLastMapEntry(FlattenedItem_ value) {
            this(value, new MutableInt(), new ArrayList<>());
        }

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

        void retract(Consumer<OutTuple_> retractedConsumer) {
            for (var i = newCount.intValue(); i < outTupleList.size(); i++) {
                retractedConsumer.accept(outTupleList.get(i));
            }
            outTupleList.subList(newCount.intValue(), outTupleList.size()).clear();
        }

        void reset() {
            newCount.setValue(0);
        }
    }

}
