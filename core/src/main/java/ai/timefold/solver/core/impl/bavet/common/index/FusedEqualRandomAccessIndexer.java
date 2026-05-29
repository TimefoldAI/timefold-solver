package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * As {@link FusedEqualLinkedListIndexer}, but backed by an {@link ElementAwareArrayList}
 * so that it supports random access (required by neighborhoods).
 * Collapses an {@link EqualIndexer} whose downstream is a {@link RandomAccessIndexerBackend}.
 *
 * @param <T> the type of tuple being indexed
 */
@NullMarked
final class FusedEqualRandomAccessIndexer<T> implements Indexer<T> {

    // See EqualIndexer for the rationale behind the initial capacity and load factor.
    private final Map<Object, ElementAwareArrayList<T>> keyToTupleList = new HashMap<>(16, 0.5f);

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var tupleList = keyToTupleList.get(compositeKey);
        if (tupleList == null) {
            tupleList = new ElementAwareArrayList<>();
            keyToTupleList.put(compositeKey, tupleList);
        }
        return tupleList.addEntry(tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var tupleList = keyToTupleList.get(compositeKey);
        if (tupleList == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        ((ElementAwareArrayList<T>.Entry) entry).remove();
        if (tupleList.isEmpty()) {
            keyToTupleList.remove(compositeKey);
        }
    }

    @Override
    public int size(Object compositeKey) {
        if (keyToTupleList.isEmpty()) {
            return 0;
        }
        var tupleList = keyToTupleList.get(compositeKey);
        return tupleList == null ? 0 : tupleList.size();
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        if (keyToTupleList.isEmpty()) {
            return;
        }
        var tupleList = keyToTupleList.get(compositeKey);
        if (tupleList != null) {
            tupleList.forEach(tupleConsumer);
        }
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        if (keyToTupleList.isEmpty()) {
            return Collections.emptyIterator();
        }
        var tupleList = keyToTupleList.get(queryCompositeKey);
        return tupleList == null ? Collections.emptyIterator() : tupleList.iterator();
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        if (keyToTupleList.isEmpty()) {
            return Collections.emptyIterator();
        }
        var tupleList = keyToTupleList.get(queryCompositeKey);
        return tupleList == null ? Collections.emptyIterator()
                : new DefaultUniqueRandomIterator<>(tupleList, workingRandom);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, Predicate<T> filter) {
        if (keyToTupleList.isEmpty()) {
            return Collections.emptyIterator();
        }
        var tupleList = keyToTupleList.get(queryCompositeKey);
        return tupleList == null ? Collections.emptyIterator()
                : new FilteredUniqueRandomIterator<>(tupleList, workingRandom, filter);
    }

    @Override
    public boolean isRemovable() {
        return keyToTupleList.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + keyToTupleList.size();
    }

}
