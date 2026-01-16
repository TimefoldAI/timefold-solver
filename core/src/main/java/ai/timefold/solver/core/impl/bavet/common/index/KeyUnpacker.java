package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Function;

/**
 * A function that retrieves keys of a composite key for an {@link Indexer}.
 * For example, {@code join(..., equals(), lessThan(), greaterThan())} has 3 keys.
 * Given {@code ("a", 7, 9)} the key retriever for {@code lessThan()} retrieves {@code 7}.
 * 
 * @param <Key_>
 */
sealed interface KeyUnpacker<Key_> extends Function<Object, Key_>
        permits CompositeKeyUnpacker, SingleKeyUnpacker {

}
