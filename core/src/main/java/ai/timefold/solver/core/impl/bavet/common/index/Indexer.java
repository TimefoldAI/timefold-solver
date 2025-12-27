package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.List;
import java.util.function.Consumer;

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
        permits EqualIndexer, ComparisonIndexer, ContainIndexer, ContainedInIndexer, IndexerBackend {

    /**
     * Modify operation.
     * 
     * @param modifyCompositeKey modify composite key, never null
     * @param tuple never null
     * @return the entry to allow remove it from the index directly, never null
     */
    ListEntry<T> put(Object modifyCompositeKey, T tuple);

    /**
     * Modify operation.
     * 
     * @param modifyCompositeKey modify composite key, never null
     * @param entry never null
     */
    void remove(Object modifyCompositeKey, ListEntry<T> entry);

    /**
     * Query operation.
     * 
     * @param queryCompositeKey query composite key, never null
     * @return at least 0
     */
    int size(Object queryCompositeKey);

    /**
     * Query operation.
     * 
     * @param queryCompositeKey query composite key, never null
     * @param tupleConsumer never null
     */
    void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer);

    /**
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Returns all entries for the given composite key as a list.
     * The index must not be modified while iterating over the returned list.
     * If the index is modified, a new instance of this list must be retrieved;
     * the previous instance is no longer valid and its behavior is undefined.
     * 
     * @param queryCompositeKey query composite key, never null
     * @return all entries for a given composite key;
     *         the caller must not modify the list
     */
    List<? extends ListEntry<T>> asList(Object queryCompositeKey);

}
