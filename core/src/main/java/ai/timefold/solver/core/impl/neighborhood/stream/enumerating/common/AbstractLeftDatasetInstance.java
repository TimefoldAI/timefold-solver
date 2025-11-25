package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractLeftDatasetInstance<Solution_, Tuple_ extends AbstractTuple>
        extends AbstractDatasetInstance<Solution_, Tuple_>
        implements Iterable<Tuple_> {

    private final ElementAwareArrayList<Tuple_> tupleList = new ElementAwareArrayList<>();

    protected AbstractLeftDatasetInstance(AbstractDataset<Solution_, Tuple_> parent, int rightMostPositionStoreIndex) {
        super(parent, rightMostPositionStoreIndex);
    }

    @Override
    public void insert(Tuple_ tuple) {
        tuple.setStore(entryStoreIndex, tupleList.add(tuple));
    }

    @Override
    public void update(Tuple_ tuple) {
        // No need to do anything.
    }

    @Override
    public void retract(Tuple_ tuple) {
        ElementAwareArrayList.Entry<Tuple_> entry = tuple.removeStore(entryStoreIndex);
        tupleList.remove(entry);
    }

    @Override
    public Iterator<Tuple_> iterator() {
        return new UnwrappingIterator<>(tupleList.asList().iterator());
    }

    public DefaultUniqueRandomSequence<Tuple_> buildRandomSequence() {
        return new DefaultUniqueRandomSequence<>(tupleList.asList());
    }

    public FilteredUniqueRandomSequence<Tuple_> buildRandomSequence(Predicate<Tuple_> predicate) {
        return new FilteredUniqueRandomSequence<>(tupleList.asList(), predicate);
    }

    public int size() {
        return tupleList.size();
    }

    record UnwrappingIterator<T>(Iterator<? extends ListEntry<T>> parentIterator)
            implements
                Iterator<T> {

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        public T next() {
            return parentIterator.next().getElement();
        }

    }

}
