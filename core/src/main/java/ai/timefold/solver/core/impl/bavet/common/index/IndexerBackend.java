package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.List;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * Bottom-most indexer in the indexer chain.
 * Constraint streams and Neighborhoods use different indexer backends,
 * as they have different performance characteristics and requirements.
 *
 * @param <T>
 */
@NullMarked
public sealed interface IndexerBackend<T>
        extends Indexer<T>
        permits RandomAccessIndexerBackend, LinkedListIndexerBackend {

    @Override
    default List<? extends ListEntry<T>> asList(Object compositeKey) {
        throw new UnsupportedOperationException("Indexer backend (%s) does not support random access."
                .formatted(this.getClass().getSimpleName()));
    }

}
