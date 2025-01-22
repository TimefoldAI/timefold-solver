package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
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

    ElementAwareListEntry<T> put(Object indexKeys, T tuple);

    void remove(Object indexKeys, ElementAwareListEntry<T> entry);

    int size(Object indexKeys);

    void forEach(Object indexKeys, Consumer<T> tupleConsumer);

    boolean isEmpty();

}
