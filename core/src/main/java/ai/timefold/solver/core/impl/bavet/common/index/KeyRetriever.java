package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Function;

// TODO Naming confusion: KeyRetriever and KeyExtractor are too similar for separate concepts.
/**
 * A function that retrieves keys of a composite key for an {@link Indexer}.
 * For example, {@code join(..., equals(), lessThan(), greaterThan())} has 3 keys.
 * Given {@code ("a", 7, 9)} the key retriever for {@code lessThan()} retrieves {@code 7}.
 * 
 * @param <Key_>
 */
sealed interface KeyRetriever<Key_> extends Function<Object, Key_>
        permits CompositeKeyRetriever, SingleKeyRetriever {

}
