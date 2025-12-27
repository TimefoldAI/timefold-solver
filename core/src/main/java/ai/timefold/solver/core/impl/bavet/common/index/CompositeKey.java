package ai.timefold.solver.core.impl.bavet.common.index;

/**
 * Cached in tuples; each tuple carries its unique instance.
 * <p>
 * Instances are shallow immutable and implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * If two instances contain elements which are equal, they must be equal.
 * <p>
 * Instances should be obtained using {@link CompositeKey#none()},
 * {@link CompositeKey#of(Object)},
 * {@link CompositeKey#of(Object, Object)}
 * or {@link CompositeKey#ofMany(Object[])}.
 * <p>
 * This interface only has two implementations,
 * giving the JVM simple bi-morphic call sites.
 * Use case of no key is handled by {@link #none()}.
 * There is also no case of a single key, see {@link #of(Object)} for rationale.
 * {@link BiCompositeKey} exists to avoid wrapping two keys with an entire array,
 * with the use case of two keys still being relatively common.
 * TriCompositeKey and higher are rare enough for {@link MegaCompositeKey} to suffice.
 */
public sealed interface CompositeKey
        permits BiCompositeKey, MegaCompositeKey {

    static CompositeKey none() {
        return MegaCompositeKey.EMPTY;
    }

    /**
     * @param key may be null, typically in cases where the indexed property is a nullable planning variable.
     * @return When the key is not {@code null}, returns the key itself,
     *         as opposed to some wrapping instance of {@link CompositeKey}.
     *         Wrapping is not necessary in this case,
     *         as the wrapper would do nothing but delegate {@link Object#equals(Object)} and {@link Object#hashCode()}
     *         to the wrapped key anyway.
     *         Avoiding the wrapper saves considerable memory and gets rid of a level of indirection.
     */
    static Object of(Object key) {
        return key == null ? MegaCompositeKey.SINGLE_NULL : key;
    }

    static <Key1_, Key2_> CompositeKey of(Key1_ key1, Key2_ key2) {
        return new BiCompositeKey<>(key1, key2);
    }

    static CompositeKey ofMany(Object... keys) {
        return new MegaCompositeKey(keys);
    }

    /**
     * Retrieves key at a given position.
     *
     * @param id Maps to a single {@link Indexer} instance in the indexer chain.
     * @return May be null if the key is null.
     * @param <Key_> {@link ComparisonIndexer} will expect this to implement {@link Comparable}.
     *        {@link EqualIndexer} will treat items as the same if they are equal.
     */
    <Key_> Key_ get(int id);

}
