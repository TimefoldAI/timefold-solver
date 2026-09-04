package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

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
 * Some indexer types (such as {@link ContainingIndexer}) have two different key types (modify key vs query key),
 * depending on the operation type (modify operation vs query operation).
 * For such an indexer the modify key is a collection, but the query key is not.
 *
 * @param <T> The element type. Often a tuple.
 *        For example for {@code from(A).join(B)}, the tuple is {@code UniTuple<A>} xor {@code UniTuple<B>}.
 *        For example for {@code Bi<A, B>.join(C)}, the tuple is {@code BiTuple<A, B>} xor {@code UniTuple<C>}.
 */
@NullMarked
public sealed interface Indexer<T>
        permits EqualIndexer, ComparisonIndexer, ContainingIndexer, ContainedInIndexer, ContainingAnyOfIndexer, LeafIndexer {

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
     * Must be exact and de-duplicated (the count of distinct matching elements),
     * not an upper bound:
     * it seeds {@code AbstractIndexedIfExistsNode}'s {@code countRight},
     * which is thereafter only incremented/decremented and compared against zero, never recomputed,
     * so an over-count would never return to zero again;
     * it also feeds {@link MultiBucketUniqueRandomIterator}'s per-bucket weights,
     * which are decremented as elements are drawn,
     * so an over-count there throws {@link NoSuchElementException} once the bucket is actually exhausted.
     * An implementation whose buckets can overlap
     * (see {@link ContainingAnyOfIndexer})
     * must de-duplicate before counting, not sum per-key bucket sizes.
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
     * Iterator which picks elements randomly, with replacement.
     * If the iterator has elements,
     * it never ends and may return the same value multiple times.
     * Selection probability is uniform over all elements for the given composite key.
     * {@link Iterator#remove()} is not supported.
     * <p>
     * This is the cheap default, and should be preferred
     * unless the caller specifically needs every element exactly once;
     * see {@link #uniqueRandomIterator(Object, RandomGenerator)} for that.
     *
     * @param queryCompositeKey composite key uniquely identifying the backend or a set of backends
     * @param workingRandom used to pick random elements
     * @return iterator for the given composite key, possibly empty
     */
    RepeatingRandomIterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom);

    /**
     * Iterator which picks elements randomly, without replacement.
     * Selection probability is uniform over all elements for the given composite key.
     * Every element for the given composite key is eventually returned exactly once,
     * and then the iterator ends without any cooperation from the caller.
     * This is significantly more expensive to create and to maintain than {@link #randomIterator(Object, RandomGenerator)},
     * since it must track which elements were already returned;
     * use it only when the caller specifically needs that guarantee.
     * <p>
     * The element is not removed from the index itself;
     * the only way to remove from the index is to call {@link #remove(Object, ListEntry)},
     * which will make any existing iterators invalid.
     *
     * @param queryCompositeKey composite key uniquely identifying the backend or a set of backends
     * @param workingRandom used to pick random elements
     * @return iterator for the given composite key, possibly empty
     */
    UniqueRandomIterator<T> uniqueRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom);

}
