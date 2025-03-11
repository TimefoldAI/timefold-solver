package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

public final class DatasetInstance<Solution_, Tuple_ extends AbstractTuple>
        implements TupleLifecycle<Tuple_> {

    private final AbstractDataset<Solution_, Tuple_> parent;
    private final int inputStoreIndex;
    private final ElementAwareList<Tuple_> tupleList = new ElementAwareList<>();

    public DatasetInstance(AbstractDataset<Solution_, Tuple_> parent, int inputStoreIndex) {
        this.parent = Objects.requireNonNull(parent);
        this.inputStoreIndex = inputStoreIndex;
    }

    public AbstractDataset<Solution_, Tuple_> getParent() {
        return parent;
    }

    @Override
    public void insert(Tuple_ tuple) {
        var entry = tupleList.add(tuple);
        tuple.setStore(inputStoreIndex, entry);
    }

    @Override
    public void update(Tuple_ tuple) {
        // No need to do anything.
    }

    @Override
    public void retract(Tuple_ tuple) {
        ElementAwareListEntry<Tuple_> entry = tuple.removeStore(inputStoreIndex);
        entry.remove();
    }

    public Iterator<Tuple_> iterator() {
        return tupleList.iterator();
    }

    public Iterator<Tuple_> iterator(Random workingRandom) {
        return tupleList.randomizedIterator(workingRandom);
    }

}
