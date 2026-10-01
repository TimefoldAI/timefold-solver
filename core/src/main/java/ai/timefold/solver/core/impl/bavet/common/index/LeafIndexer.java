package ai.timefold.solver.core.impl.bavet.common.index;

import org.jspecify.annotations.NullMarked;

/**
 * Bottom-most indexer in the indexer chain.
 * Constraint streams and Neighborhoods use different leaf indexers,
 * as they have different performance characteristics and requirements.
 *
 * @param <T>
 */
@NullMarked
public sealed interface LeafIndexer<T>
        extends Indexer<T>
        permits RandomAccessLeafIndexer, LinkedListLeafIndexer {

}
