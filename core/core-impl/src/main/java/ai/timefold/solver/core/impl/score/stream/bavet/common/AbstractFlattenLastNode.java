package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;

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
            List<OutTuple_> outTupleList = new ArrayList<>(size);
            for (FlattenedItem_ item : collection) {
                addTuple(tuple, item, outTupleList);
            }
            tuple.setStore(flattenLastStoreIndex, outTupleList);
        } else {
            Iterator<FlattenedItem_> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return;
            }
            List<OutTuple_> outTupleList = new ArrayList<>();
            while (iterator.hasNext()) {
                addTuple(tuple, iterator.next(), outTupleList);
            }
            tuple.setStore(flattenLastStoreIndex, outTupleList);
        }
    }

    private void addTuple(InTuple_ originalTuple, FlattenedItem_ item, List<OutTuple_> outTupleList) {
        OutTuple_ tuple = createTuple(originalTuple, item);
        outTupleList.add(tuple);
        propagationQueue.insert(tuple);
    }

    protected abstract OutTuple_ createTuple(InTuple_ originalTuple, FlattenedItem_ item);

    @Override
    public final void update(InTuple_ tuple) {
        List<OutTuple_> outTupleList = tuple.getStore(flattenLastStoreIndex);
        if (outTupleList == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s).
            insert(tuple);
            return;
        }
        Iterable<FlattenedItem_> iterable = mappingFunction.apply(getEffectiveFactIn(tuple));
        List<FlattenedItem_> newFlattenedItemList = iterableToList(iterable);
        if (newFlattenedItemList.isEmpty()) { // Everything has to be removed.
            retract(tuple);
            return;
        }
        if (!outTupleList.isEmpty()) {
            // Remove all facts from the input that already have an out tuple.
            Iterator<OutTuple_> outTupleIterator = outTupleList.iterator();
            while (outTupleIterator.hasNext()) {
                OutTuple_ outTuple = outTupleIterator.next();
                FlattenedItem_ existingFlattenedItem = getEffectiveFactOut(outTuple);
                boolean existsAlsoInNew = false;
                if (!newFlattenedItemList.isEmpty()) {
                    // A fact can be present more than once and every iteration should only remove one instance.
                    Iterator<FlattenedItem_> newFlattenedItemIterator = newFlattenedItemList.iterator();
                    while (newFlattenedItemIterator.hasNext()) {
                        FlattenedItem_ newFlattenedItem = newFlattenedItemIterator.next();
                        // We check for identity, not equality, to not introduce dependency on user equals().
                        if (newFlattenedItem == existingFlattenedItem) {
                            // Remove item from the list, as it means its tuple need not be added later.
                            newFlattenedItemIterator.remove();
                            existsAlsoInNew = true;
                            break;
                        }
                    }
                }
                if (!existsAlsoInNew) {
                    outTupleIterator.remove();
                    removeTuple(outTuple);
                } else {
                    propagationQueue.update(outTuple);
                }
            }
        }
        // Whatever is left in the input needs to be added.
        for (FlattenedItem_ newFlattenedItem : newFlattenedItemList) {
            addTuple(tuple, newFlattenedItem, outTupleList);
        }
    }

    private List<FlattenedItem_> iterableToList(Iterable<FlattenedItem_> iterable) {
        if (iterable instanceof Collection<FlattenedItem_> collection) {
            // Optimization for Collection, where we know the size.
            int size = collection.size();
            if (size == 0) {
                return Collections.emptyList();
            }
            List<FlattenedItem_> result = new ArrayList<>(size);
            iterable.forEach(result::add);
            return result;
        } else {
            Iterator<FlattenedItem_> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return Collections.emptyList();
            }
            List<FlattenedItem_> result = new ArrayList<>();
            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
            return result;
        }
    }

    protected abstract EffectiveItem_ getEffectiveFactIn(InTuple_ tuple);

    protected abstract FlattenedItem_ getEffectiveFactOut(OutTuple_ outTuple);

    @Override
    public final void retract(InTuple_ tuple) {
        List<OutTuple_> outTupleList = tuple.removeStore(flattenLastStoreIndex);
        if (outTupleList == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        for (OutTuple_ item : outTupleList) {
            removeTuple(item);
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

}
