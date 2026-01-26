package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * An indexer for entity or fact {@code X},
 * maps a property or a combination of properties of {@code X}, denoted by {@code compositeKey},
 * to all instances of {@code X} that match those properties,
 * depending on the indexer type (equal, lower than, contain, ...).
 * For example for {@code {Lesson(id=1, room=A), Lesson(id=2, room=B), Lesson(id=3, room=A)}},
 * calling {@code visit(room=A)} would visit lesson 1 and 3.
 * <p>
 * The fact X is wrapped in a Tuple, because the {@link TupleState} is needed by clients of
 * {@link #forEach(Object, Consumer)}.
 * <p>
 * Some indexer types (such as contain, containedIn, ...) have two different key types (modify key vs query key),
 * depending on the operation type (modify operation vs query operation).
 * For example, for a contain indexer the modify key is a collection, but the query key is not.
 *
 * @param <T> The element type. Often a tuple.
 *        For example for {@code from(A).join(B)}, the tuple is {@code UniTuple<A>} xor {@code UniTuple<B>}.
 *        For example for {@code Bi<A, B>.join(C)}, the tuple is {@code BiTuple<A, B>} xor {@code UniTuple<C>}.
 */
@NullMarked
public sealed interface Indexer<T>
        permits EqualIndexer, ComparisonIndexer, ContainingIndexer, ContainedInIndexer, ContainingAnyOfIndexer, IndexerBackend {

    /**
     * Modify operation.
     *
     * @param modifyCompositeKey modify composite key
     * @param tuple never null
     * @return the entry to allow remove it from the index directly
     */
    ListEntry<T> put(Object modifyCompositeKey, T tuple);

    /**
     * Modify operation.
     * Must not be called during {@link #forEach(Object, Consumer)}
     * and invalidates any {@link #iterator(Object)} obtained before.
     *
     * @param modifyCompositeKey modify composite key
     * @param entry never null
     */
    void remove(Object modifyCompositeKey, ListEntry<T> entry);

    /**
     * Query operation.
     *
     * @param queryCompositeKey query composite key
     * @return at least 0
     */
    int size(Object queryCompositeKey);

    /**
     * Query operation.
     *
     * @param queryCompositeKey query composite key
     * @param tupleConsumer never null
     */
    default void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        var iterator = iterator(queryCompositeKey);
        while (iterator.hasNext()) {
            tupleConsumer.accept(iterator.next());
        }
    }

    /**
     * Gets an iterator for the given composite key.
     * The returned iterator does not support {@link Iterator#remove()}.
     *
     * @param queryCompositeKey composite key uniquely identifying the backend or a set of backends
     * @return possibly empty iterator for the given composite key
     */
    Iterator<T> iterator(Object queryCompositeKey);

    /**
     * Some indexers can be empty (size 0 and an empty forEach for all keys)
     * but not yet removable.
     *
     * @return true if empty and all put() calls had a remove() call
     */
    boolean isRemovable();

    /**
     * Iterator which picks elements randomly.
     * Selection probability is uniform over all elements for the given composite key.
     * By calling {@link Iterator#remove()},
     * the element is removed never to be returned again by this iterator.
     * However, it is not removed from the index itself;
     * the only way to remove from the index is to call {@link #remove(Object, ListEntry)},
     * which will make any existing iterators invalid.
     *
     * @param queryCompositeKey composite key uniquely identifying the backend or a set of backends
     * @param workingRandom used to pick random elements
     * @return iterator for the given composite key, possibly empty
     */
    Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom);

    /**
     * As defined by {@link #randomIterator(Object, Random)},
     * but only returning elements matching the given filter.
     */
    Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom, Predicate<T> filter);

}
