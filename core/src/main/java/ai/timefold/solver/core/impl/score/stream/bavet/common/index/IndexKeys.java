package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

/**
 * Cached in tuples; each tuple carries its unique instance.
 * <p>
 * Instances are shallow immutable and implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * If two instances contain elements which are equal, they must be equal.
 * <p>
 * Instances should be obtained using {@link IndexKeys#none()},
 * {@link IndexKeys#of(Object)},
 * {@link IndexKeys#of(Object, Object)},
 * {@link IndexKeys#of(Object, Object, Object)}
 * or {@link IndexKeys#ofMany(Object[])}.
 */
public sealed interface IndexKeys
        permits ManyIndexKeys, ThreeIndexKeys, TwoIndexKeys {

    static IndexKeys none() {
        return ManyIndexKeys.EMPTY;
    }

    /**
     * @param key may be null, typically in cases where the indexed property is a nullable planning variable.
     * @return When the key is not {@code null}, returns the key itself,
     *         as opposed to some wrapping instance of {@link IndexKeys}.
     *         Wrapping is not necessary in this case,
     *         as the wrapper would do nothing but delegate {@link Object#equals(Object)} and {@link Object#hashCode()}
     *         to the wrapped key anyway.
     *         Avoiding the wrapper saves considerable memory and gets rid of a level of indirection.
     */
    static Object of(Object key) {
        return key == null ? ManyIndexKeys.SINGLE_NULL : key;
    }

    static <Key1_, Key2_> IndexKeys of(Key1_ key1, Key2_ key2) {
        return new TwoIndexKeys<>(key1, key2);
    }

    static <Key1_, Key2_, Key3_> IndexKeys of(Key1_ key1, Key2_ key2, Key3_ key3) {
        return new ThreeIndexKeys<>(key1, key2, key3);
    }

    static IndexKeys ofMany(Object... keys) {
        return new ManyIndexKeys(keys);
    }

    /**
     * Retrieves key at a given position.
     *
     * @param id Maps to a single {@link Indexer} instance in the indexer chain.
     * @return never null
     * @param <Type_> {@link ComparisonIndexer} will expect this to implement {@link Comparable}.
     *        {@link EqualsIndexer} will treat items as the same if they are equal.
     */
    <Type_> Type_ get(int id);

}
