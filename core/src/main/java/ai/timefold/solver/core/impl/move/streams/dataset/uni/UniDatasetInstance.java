package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniDatasetInstance<Solution_, A>
        extends AbstractDatasetInstance<Solution_, UniTuple<A>> {

    private final ElementAwareList<UniTuple<A>> tupleList = new ElementAwareList<>();

    public UniDatasetInstance(AbstractDataset<Solution_, UniTuple<A>> parent, int inputStoreIndex) {
        super(parent, inputStoreIndex);
    }

    @Override
    public void insert(UniTuple<A> tuple) {
        var entry = tupleList.add(tuple);
        tuple.setStore(inputStoreIndex, entry);
    }

    @Override
    public void retract(UniTuple<A> tuple) {
        ElementAwareListEntry<UniTuple<A>> entry = tuple.removeStore(inputStoreIndex);
        entry.remove();
    }

    public Iterator<UniTuple<A>> iterator() {
        return tupleList.iterator();
    }

    public Iterator<UniTuple<A>> iterator(Random workingRandom) {
        return tupleList.randomizedIterator(workingRandom);
    }

}
