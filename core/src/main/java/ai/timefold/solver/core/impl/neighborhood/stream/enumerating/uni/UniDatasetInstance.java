package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.UniqueRandomSequence;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UniDatasetInstance<Solution_, A>
        extends AbstractDatasetInstance<Solution_, UniTuple<A>> {

    private final ArrayList<UniTuple<A>> tupleList = new ArrayList<>();

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
    public UniqueRandomSequence<UniTuple<A>> buildRandomSequence(@Nullable Predicate<UniTuple<A>> predicate) {
        if (tupleList.isEmpty()) {
            return UniqueRandomSequence.empty();
        }
        // The following code is based on the assumption that most of the time, these lists will be large.
        // (Thousands, tens of thousands of elements.)
        // Therefore this implementation optimizes to avoid copying lists.
        if (predicate == null) {
            return new UniqueRandomSequence<>(tupleList);
        }
        var tupleCount = tupleList.size();
        var irrelevantTupleIndexSet = new BitSet(tupleCount);
        for (var i = 0; i < tupleCount; i++) {
            var tuple = tupleList.get(i);
            if (!predicate.test(tuple)) {
                irrelevantTupleIndexSet.set(i);
            }
        }
        return new UniqueRandomSequence<>(tupleList, irrelevantTupleIndexSet);
    }

    @Override
    public int size() {
        return tupleList.size();
    }

}
