package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DefaultUniqueRandomSequence;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.FilteredUniqueRandomSequence;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniDatasetInstance<Solution_, A>
        extends AbstractDatasetInstance<Solution_, UniTuple<A>> {

    private final List<UniTuple<A>> tupleList = new ArrayList<>();

    public UniDatasetInstance(AbstractDataset<Solution_, UniTuple<A>> parent, int rightMostPositionStoreIndex) {
        super(parent, rightMostPositionStoreIndex);
    }

    @Override
    public void insert(UniTuple<A> tuple) {
        tupleList.add(tuple);
        // Since elements are only ever added at the end,
        // the index is always the right-most position that the tuple could be found at.
        var rightMostIndex = tupleList.size() - 1;
        tuple.setStore(entryStoreIndex, rightMostIndex);
    }

    @Override
    public void update(UniTuple<A> tuple) {
        // No need to do anything.
    }

    @Override
    public void retract(UniTuple<A> tuple) {
        // The tuple knows the right-most index it could be found at.
        // But retracts may have shifted other tuples to the left,
        // so we need to search backwards from there.
        // Thankfully retracts are relatively rare.
        int rightMostIndex = Math.min(tuple.removeStore(entryStoreIndex), tupleList.size() - 1);
        for (int i = rightMostIndex; i >= 0; i--) {
            if (tupleList.get(i) == tuple) {
                tupleList.remove(i);
                return;
            }
        }
        throw new IllegalStateException("Impossible state: tuple (%s) not found."
                .formatted(tuple));
    }

    @Override
    public Iterator<UniTuple<A>> iterator() {
        return tupleList.iterator();
    }

    @Override
    public DefaultUniqueRandomSequence<UniTuple<A>> buildRandomSequence() {
        return new DefaultUniqueRandomSequence<>(tupleList);
    }

    @Override
    public FilteredUniqueRandomSequence<UniTuple<A>> buildRandomSequence(Predicate<UniTuple<A>> predicate) {
        return new FilteredUniqueRandomSequence<>(tupleList, predicate);
    }

    @Override
    public int size() {
        return tupleList.size();
    }

}
