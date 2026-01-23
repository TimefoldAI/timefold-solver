package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * An {@link IndexerBackend} that supports random access to its entries.
 * It is shown to be 10-20 % slower than {@link LinkedListIndexerBackend} in the micro benchmarks
 * when used as the backend for constraint streams.
 *
 * @param <T> the type of tuple being indexed
 */
@NullMarked
public final class RandomAccessIndexerBackend<T> implements IndexerBackend<T> {

    private final ElementAwareArrayList<T> tupleList = new ElementAwareArrayList<>();

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        return tupleList.add(tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        tupleList.remove((ElementAwareArrayList.Entry<T>) entry);
    }

    @Override
    public int size(Object compositeKey) {
        return tupleList.size();
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        tupleList.forEach(tupleConsumer);
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        return tupleList.iterator();
    }

    @Override
    public Iterator<T> randomIterator(Object compositeKey, Random workingRandom) {
        return new DefaultUniqueRandomSequence<>(tupleList, workingRandom);
    }

    @Override
    public Iterator<T> randomIterator(Object compositeKey, Random workingRandom, Predicate<T> filter) {
        return new FilteredUniqueRandomSequence<>(tupleList, workingRandom, filter);
    }

    @Override
    public boolean isRemovable() {
        return tupleList.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + tupleList.size();
    }

}
