package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

/**
 * Cached in tuples; each tuple carries its unique instance.
 * <p>
 * Instances are shallow immutable and implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * If two instances contain elements which are equal, they must be equal.
 */
public sealed interface IndexProperties
        permits ManyIndexProperties, NoneIndexProperties, SingleIndexProperties, ThreeIndexProperties, TwoIndexProperties {

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
