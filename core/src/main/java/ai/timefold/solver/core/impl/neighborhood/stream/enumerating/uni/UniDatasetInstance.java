package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class UniDatasetInstance<Solution_, A>
        extends AbstractDatasetInstance<Solution_, UniTuple<A>> {

    private final ElementAwareLinkedList<UniTuple<A>> tupleList = new ElementAwareLinkedList<>();

    public UniDatasetInstance(AbstractDataset<Solution_, UniTuple<A>> parent, int inputStoreIndex) {
        super(parent, inputStoreIndex);
    }

    @Override
    public void insert(UniTuple<A> tuple) {
        var entry = tupleList.add(tuple);
        tuple.setStore(inputStoreIndex, entry);
    }

    @Override
    public void update(UniTuple<A> tuple) {
        // No need to do anything.
    }

    @Override
    public void retract(UniTuple<A> tuple) {
        ElementAwareLinkedList.Entry<UniTuple<A>> entry = tuple.removeStore(inputStoreIndex);
        entry.remove();
    }

    public Iterator<UniTuple<A>> iterator() {
        return tupleList.iterator();
    }

    public Iterator<UniTuple<A>> iterator(Random workingRandom) {
        return tupleList.randomizedIterator(workingRandom);
    }

}
