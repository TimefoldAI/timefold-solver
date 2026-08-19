package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link Joiners#containing(Function, Function)}
 */
@NullMarked
final class ContainingIndexer<T, Key_, KeyCollection_ extends SequencedCollection<Key_>> implements Indexer<T> {

    private final KeyUnpacker<KeyCollection_> modifyKeyUnpacker;
    private final KeyUnpacker<Key_> queryKeyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    /**
     * See {@link EqualIndexer} for explanation of the parameters.
     */
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>(16, 0.5f);
    private long unremovedSize = 0;

    /**
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    @SuppressWarnings("unchecked")
    public ContainingIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
        this.queryKeyUnpacker = Objects.requireNonNull(keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        unremovedSize++;
        var indexKeyCollection = modifyKeyUnpacker.apply(modifyCompositeKey);
        return ContainingAnyOfIndexer.put(modifyCompositeKey, downstreamIndexerMap, downstreamIndexerSupplier, tuple,
                indexKeyCollection);
    }

    @Override
    public void remove(Object modifyCompositeKey, ListEntry<T> entry) {
        unremovedSize--;
        var indexKeyCollection = modifyKeyUnpacker.apply(modifyCompositeKey);
        ContainingAnyOfIndexer.remove(modifyCompositeKey, downstreamIndexerMap, entry, indexKeyCollection);
    }

    @Override
    public int size(Object queryCompositeKey) {
        if (downstreamIndexerMap.isEmpty()) {
            return 0;
        }
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(queryCompositeKey);
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        var downstreamIndexer = queryKeyUnpacker.findDownstream(downstreamIndexerMap, queryCompositeKey);
        if (downstreamIndexer != null) {
            downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
        }
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        var downstreamIndexer = queryKeyUnpacker.findDownstream(downstreamIndexerMap, queryCompositeKey);
        return downstreamIndexer == null ? Collections.emptyIterator() : downstreamIndexer.iterator(queryCompositeKey);
    }

    @Override
    public RepeatingRandomIterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var downstreamIndexer = queryKeyUnpacker.findDownstream(downstreamIndexerMap, queryCompositeKey);
        if (downstreamIndexer == null) {
            return RepeatingRandomIterator.empty();
        }
        return downstreamIndexer.randomIterator(queryCompositeKey, workingRandom);
    }

    @Override
    public UniqueRandomIterator<T> uniqueRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var downstreamIndexer = queryKeyUnpacker.findDownstream(downstreamIndexerMap, queryCompositeKey);
        if (downstreamIndexer == null) {
            return UniqueRandomIterator.empty();
        }
        return downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom);
    }

    @Override
    public boolean isRemovable() {
        return unremovedSize == 0;
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
