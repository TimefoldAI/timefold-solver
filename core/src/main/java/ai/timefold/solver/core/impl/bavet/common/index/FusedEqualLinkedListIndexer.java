package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * Collapses an {@link EqualIndexer} whose downstream is a {@link LinkedListIndexerBackend}
 * into a single map from the composite key directly to the tuple list.
 * Applies only to the leaf-most equal indexer of an all-equal chain,
 * where the index key is identical to the composite key (see {@link SingleKeyUnpacker}),
 * so neither the {@link KeyUnpacker} indirection nor a per-key backend object is needed.
 * <p>
 * Does not support random access; see {@link FusedEqualRandomAccessIndexer} for that.
 *
 * @param <T> the type of tuple being indexed
 */
@NullMarked
final class FusedEqualLinkedListIndexer<T> implements Indexer<T> {

    // See EqualIndexer for the rationale behind the initial capacity and load factor.
    private final Map<Object, ElementAwareLinkedList<T>> keyToTupleList = new HashMap<>(16, 0.5f);

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var tupleList = keyToTupleList.get(compositeKey);
        if (tupleList == null) {
            tupleList = new ElementAwareLinkedList<>();
            keyToTupleList.put(compositeKey, tupleList);
        }
        return tupleList.add(tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var tupleList = keyToTupleList.get(compositeKey);
        if (tupleList == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        tupleList.remove((ElementAwareLinkedList.Entry<T>) entry);
        if (tupleList.size() == 0) {
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
        throw new UnsupportedOperationException("Impossible state: This indexer does not support random access.");
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, Predicate<T> filter) {
        throw new UnsupportedOperationException("Impossible state: This indexer does not support random access.");
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
