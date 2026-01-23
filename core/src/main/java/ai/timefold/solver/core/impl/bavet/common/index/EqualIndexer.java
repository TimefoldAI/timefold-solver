package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class EqualIndexer<T, Key_> implements Indexer<T> {

    private final KeyUnpacker<Key_> keyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    /**
     * The number 16 is chosen as that is the default initial capacity of a HashMap
     * and we have no good way of estimating the number of keys up-front.
     * Any reasonable problem will quickly resize the map, and by a lot.
     * Since the solver is typically a long-running process,
     * this initial overhead is negligible in the grand scheme of things.
     * <p>
     * 0.5f has been established experimentally as a good load factor for this map,
     * balancing its memory consumption with lookup speed.
     * On index-heavy problems, higher load factors were observed to lead to significant lookup slowdowns.
     * Even lower load factors (0.25, 0.1) were tested and yielded further performance improvements,
     * but the memory consumption impact was deemed too high to justify it.
     * This trade-off may change with future versions of the JDK,
     * and should be re-evaluated occasionally.
     */
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>(16, 0.5f);

    /**
     * @param keyUnpacker determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public EqualIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyUnpacker = Objects.requireNonNull(keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        var indexKey = keyUnpacker.apply(compositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            downstreamIndexerMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(compositeKey, tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = getDownstreamIndexer(compositeKey, indexKey, entry);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            downstreamIndexerMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey, ListEntry<T> entry) {
        var downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(compositeKey);
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return;
        }
        downstreamIndexer.forEach(compositeKey, tupleConsumer);
    }

    @Override
    public boolean isRemovable() {
        return downstreamIndexerMap.isEmpty();
    }

    public Iterator<T> iterator(Object queryCompositeKey) {
        var indexKey = keyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyIterator();
        }
        return downstreamIndexer.iterator(queryCompositeKey);
    }

    @Override
    public Iterator<T> randomIterator(Object compositeKey, Random workingRandom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> randomIterator(Object compositeKey, Random workingRandom, Predicate<T> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
