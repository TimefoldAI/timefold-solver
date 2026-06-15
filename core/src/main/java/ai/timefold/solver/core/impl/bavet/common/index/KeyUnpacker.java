package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Function;

/**
 * A function that retrieves keys of a composite key for an {@link Indexer}.
 * For example, {@code join(..., equals(), lessThan(), greaterThan())} has 3 keys.
 * Given {@code ("a", 7, 9)} the key unpacker for {@code lessThan()} retrieves {@code 7}.
 * 
 * @param <Key_>
 */
@FunctionalInterface
interface KeyUnpacker<Key_> extends Function<Object, Key_> {

    static <Key_> KeyUnpacker<Key_> single() {
        return a -> (Key_) a;
    }

    static <Key_> KeyUnpacker<Key_> composite(int index) {
        return a -> ((CompositeKey) a).get(index);
    }

}
