package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

/**
 * Cached in tuples; each tuple carries its unique instance.
 * <p>
 * Instances are shallow immutable and implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * If two instances contain elements which are equal, they must be equal.
 */
public sealed interface IndexProperties
        permits ManyIndexProperties, SingleIndexProperties, ThreeIndexProperties, TwoIndexProperties {

    static IndexProperties none() {
        return ManyIndexProperties.EMPTY;
    }

    static <A> IndexProperties single(A a) {
        return a == null ? SingleIndexProperties.NULL : new SingleIndexProperties<>(a);
    }

    static <A, B> IndexProperties two(A a, B b) {
        return new TwoIndexProperties<>(a, b);
    }

    static <A, B, C> IndexProperties three(A a, B b, C c) {
        return new ThreeIndexProperties<>(a, b, c);
    }

    static IndexProperties many(Object... properties) {
        return new ManyIndexProperties(properties);
    }

    /**
     * Retrieves index property at a given position.
     *
     * @param id Maps to a single {@link Indexer} instance in the indexer chain.
     * @return never null
     * @param <Type_> {@link ComparisonIndexer} will expect this to implement {@link Comparable}.
     *        {@link EqualsIndexer} will treat items as the same if they are equal.
     */
    <Type_> Type_ toKey(int id);

}
