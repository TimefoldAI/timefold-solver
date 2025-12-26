package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ContainIndexer<T, Key_, CollectionKey extends Collection<Key_>> implements Indexer<T> {

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;

    /**
     * @param keyRetriever determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public ContainIndexer(KeyRetriever<Key_> keyRetriever, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyRetriever = Objects.requireNonNull(keyRetriever);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        throw new UnsupportedOperationException();
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey, ListEntry<T> entry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size(Object compositeKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ListEntry<T>> asList(Object compositeKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

}
