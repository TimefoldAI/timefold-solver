package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.RepeatingRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractLeftDatasetInstance<Solution_, Tuple_ extends Tuple>
        extends AbstractDatasetInstance<Solution_, Tuple_>
        implements UniDatasetInstance<Tuple_>, Iterable<Tuple_> {

    private final ElementAwareArrayList<Tuple_> tupleList = new ElementAwareArrayList<>();

    protected AbstractLeftDatasetInstance(AbstractDataset<Solution_> parent, int entryStoreIndex) {
        super(parent, entryStoreIndex);
    }

    @Override
    public void insert(Tuple_ tuple) {
        if (tuple.getStore(entryStoreIndex) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(tuple));
        }

        tuple.setStore(entryStoreIndex, tupleList.addEntry(tuple));
    }

    @Override
    public void update(Tuple_ tuple) {
        if (tuple.getStore(entryStoreIndex) == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple);
        } else {
            // No need to do anything.
        }
    }

    @Override
    public void retract(Tuple_ tuple) {
        ElementAwareArrayList<Tuple_>.Entry entry = tuple.removeStore(entryStoreIndex);
        if (entry == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        entry.remove();
    }

    /**
     * Not part of {@link UniDatasetInstance}: only satisfies {@link Iterable},
     * for callers (such as {@code JustInTimeBiDatasetInstance#size()})
     * that need a plain, non-random walk internally.
     */
    @Override
    public Iterator<Tuple_> iterator() {
        return tupleList.iterator();
    }

    @Override
    public Iterator<Tuple_> iterator(RandomGenerator workingRandom) {
        return RepeatingRandomIterator.of(tupleList, workingRandom);
    }

    @Override
    public UniqueRandomIterator<Tuple_> exhaustiveIterator(RandomGenerator workingRandom) {
        return UniqueRandomIterator.of(tupleList, workingRandom);
    }

    /**
     * As defined by {@link #exhaustiveIterator(RandomGenerator)},
     * but the caller must call {@link RetiringRandomIterator#retire()} itself
     * after each {@link Iterator#next()} to permanently drop an element.
     * Only meant for a caller which must decide by itself
     * when an element is no longer needed,
     * such as the left side of a join,
     * which must not retire a tuple until its right side is confirmed empty.
     */
    public RetiringRandomIterator<Tuple_> retiringRandomIterator(RandomGenerator workingRandom) {
        return RetiringRandomIterator.of(tupleList, workingRandom);
    }

    @Override
    public int size() {
        return tupleList.size();
    }

}
