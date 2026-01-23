package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractLeftDatasetInstance<Solution_, Tuple_ extends Tuple>
        extends AbstractDatasetInstance<Solution_, Tuple_>
        implements Iterable<Tuple_> {

    private final ElementAwareArrayList<Tuple_> tupleList = new ElementAwareArrayList<>();
    private final int rightIteratorStoreIndex;

    protected AbstractLeftDatasetInstance(AbstractDataset<Solution_> parent, int rightIteratorStoreIndex, int entryStoreIndex) {
        super(parent, entryStoreIndex);
        this.rightIteratorStoreIndex = rightIteratorStoreIndex;
    }

    public int getRightIteratorStoreIndex() {
        return rightIteratorStoreIndex;
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
        tupleList.remove(tuple.removeStore(entryStoreIndex));
    }

    @Override
    public Iterator<Tuple_> iterator() {
        return tupleList.iterator();
    }

    public Iterator<Tuple_> randomIterator(Random workingRandom) {
        return UniqueRandomIterator.of(tupleList, workingRandom);
    }

    public int size() {
        return tupleList.size();
    }

}
