package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

/**
 * An indexer for entity or fact {@code X},
 * maps a property or a combination of properties of {@code X}
 * (keys denoted by {@code indexProperties})
 * to all instances of {@code X} that match those properties,
 * depending on the indexer type (equal, lower than, ...).
 * <p>
 * Example: for {@code {Lesson(id=1, room=A), Lesson(id=2, room=B), Lesson(id=3, room=A)}}
 * and an index on {@code Lesson::room},
 * calling {@code EqualsIndexer.forEach(A)} would visit lesson 1 and 3,
 * room "A" being the key.
 * <p>
 * The fact X is wrapped in a Tuple, because the {@link TupleState} is needed by clients of
 * {@link #forEach(Object, Consumer)}.
 * <p>
 * Index keys are typically provided by {@link IndexProperties}.
 * The only exception is when there is only one key, in which case the key is provided directly.
 * This is to alleviate the GC pressure in that case, as this simple wrapper provides no additional value.
 * In this case, the single key needs to respect the requirements of {@link IndexProperties}.
 *
 * @param <T> The element type. Often a tuple.
 *        For example for {@code from(A).join(B)}, the tuple is {@code UniTuple<A>} xor {@code UniTuple<B>}.
 *        For example for {@code Bi<A, B>.join(C)}, the tuple is {@code BiTuple<A, B>} xor {@code UniTuple<C>}.
 */
public sealed interface Indexer<T> permits ComparisonIndexer, EqualsIndexer, NoneIndexer {

    @SuppressWarnings("unchecked")
    static <Key_> Key_ extractKey(Object indexPropertiesObject, int propertyIndex) {
        if (indexPropertiesObject instanceof IndexProperties indexProperties) {
            return indexProperties.toKey(propertyIndex);
        }
        return (Key_) indexPropertiesObject;
    }

    ElementAwareListEntry<T> put(Object indexProperties, T tuple);

    void remove(Object indexProperties, ElementAwareListEntry<T> entry);

    int size(Object indexProperties);

    void forEach(Object indexProperties, Consumer<T> tupleConsumer);

    boolean isEmpty();

}
