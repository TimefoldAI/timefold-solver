package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Iterator;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DefaultUniqueRandomSequence;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.FilteredUniqueRandomSequence;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniDatasetInstance<Solution_, A>
        extends AbstractDatasetInstance<Solution_, UniTuple<A>> {

    private final ElementAwareArrayList<UniTuple<A>> tupleList = new ElementAwareArrayList<>();

    public UniDatasetInstance(AbstractDataset<Solution_, UniTuple<A>> parent, int rightMostPositionStoreIndex) {
        super(parent, rightMostPositionStoreIndex);
    }

    @Override
    public void insert(UniTuple<A> tuple) {
        tuple.setStore(entryStoreIndex, tupleList.add(tuple));
    }

    @Override
    public void update(UniTuple<A> tuple) {
        // No need to do anything.
    }

    @Override
    public void retract(UniTuple<A> tuple) {
        ElementAwareArrayList.Entry<UniTuple<A>> entry = tuple.removeStore(entryStoreIndex);
        tupleList.remove(entry);
    }

    @Override
    public Iterator<UniTuple<A>> iterator() {
        return new UnwrappingIterator<>(tupleList.asList().iterator());
    }

    @Override
    public DefaultUniqueRandomSequence<UniTuple<A>> buildRandomSequence() {
        return new DefaultUniqueRandomSequence<>(tupleList.asList());
    }

    @Override
    public FilteredUniqueRandomSequence<UniTuple<A>> buildRandomSequence(Predicate<UniTuple<A>> predicate) {
        return new FilteredUniqueRandomSequence<>(tupleList.asList(), predicate);
    }

    @Override
    public int size() {
        return tupleList.size();
    }

    private static final class UnwrappingIterator<T> implements Iterator<T> {

        private final Iterator<ElementAwareArrayList.Entry<T>> parentIterator;

        public UnwrappingIterator(Iterator<ElementAwareArrayList.Entry<T>> parentIterator) {
            this.parentIterator = parentIterator;
        }

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
