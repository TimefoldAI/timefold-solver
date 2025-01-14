package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * An indexer for entity or fact {@code X},
 * maps a property or a combination of properties of {@code X}, denoted by {@code indexKeys},
 * to all instances of {@code X} that match those properties,
 * depending on the the indexer type (equal, lower than, ...).
 * For example for {@code {Lesson(id=1, room=A), Lesson(id=2, room=B), Lesson(id=3, room=A)}},
 * calling {@code visit(room=A)} would visit lesson 1 and 3.
 * <p>
 * The fact X is wrapped in a Tuple, because the {@link TupleState} is needed by clients of
 * {@link #forEach(Object, Consumer)}.
 *
 * @param <T> The element type. Often a tuple.
 *        For example for {@code from(A).join(B)}, the tuple is {@code UniTuple<A>} xor {@code UniTuple<B>}.
 *        For example for {@code Bi<A, B>.join(C)}, the tuple is {@code BiTuple<A, B>} xor {@code UniTuple<C>}.
 */
public sealed interface Indexer<T> permits ComparisonIndexer, EqualsIndexer, NoneIndexer {

    /**
     * Retrieves the key at a given position.
     *
     * @param indexKeys The key(s) available to the indexer.
     * @param id The position of the key.
     * @return The key at the given position.
     * @param <Key_> The type of the key.
     * @see IndexKeys#of(Object) Description of why indexKeys sometimes isn't an instance of class IndexKeys.
     */
    static <Key_> Key_ of(Object indexKeys, int id) {
        if (indexKeys instanceof IndexKeys keys) {
            return keys.get(id);
        } else if (id != 0) {
            throw new IllegalArgumentException("Impossible state: the index is a single key (%s), yet the id is not zero (%d)."
                    .formatted(indexKeys, id));
        }
        return (Key_) indexKeys;
    }

    ElementAwareListEntry<T> put(Object indexKeys, T tuple);

    void remove(Object indexKeys, ElementAwareListEntry<T> entry);

    int size(Object indexKeys);

    void forEach(Object indexKeys, Consumer<T> tupleConsumer);

    boolean isEmpty();

}
