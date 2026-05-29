package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A unified beta-memory index for an equal-bearing join/ifExists node, replacing the two parallel
 * left/right equal indexers with ONE shared map from the equal-prefix key to a {@link JoinBucket}.
 * <p>
 * An EQUAL match is {@code Objects.equals(leftKey, rightKey)} — exactly how a {@link HashMap} groups
 * entries — so matching left and right tuples land in the SAME bucket; co-location IS the match.
 * One top-level lookup per insert then replaces the second (cross-side) lookup the two-indexer path does.
 * <p>
 * The map is keyed by {@code topEqualKeyUnpacker.apply(compositeKey)}: the whole composite key when the
 * join is pure-equal (a single merged level), or the key's first component when there is a
 * comparison/containing suffix. Callers pass the FULL composite key to every method; the unpacking to the
 * equal key happens here, and the suffix levels are handled by the bucket's per-side downstream indexers.
 *
 * @param <L> the left element type (a left tuple, or {@code ExistsCounter} for ifExists)
 * @param <R> the right element type (a right {@code UniTuple})
 */
@NullMarked
public final class JoinIndex<L, R> {

    // See EqualIndexer for the rationale behind the initial capacity and load factor.
    private final Map<Object, JoinBucket<L, R>> bucketMap = new HashMap<>(16, 0.5f);
    private final KeyUnpacker<Object> topEqualKeyUnpacker;
    private final Supplier<Indexer<L>> leftDownstreamSupplier;
    private final Supplier<Indexer<R>> rightDownstreamSupplier;

    // Package-private: only IndexerFactory#buildJoinIndex (same package) creates a JoinIndex.
    JoinIndex(KeyUnpacker<Object> topEqualKeyUnpacker, Supplier<Indexer<L>> leftDownstreamSupplier,
            Supplier<Indexer<R>> rightDownstreamSupplier) {
        this.topEqualKeyUnpacker = topEqualKeyUnpacker;
        this.leftDownstreamSupplier = leftDownstreamSupplier;
        this.rightDownstreamSupplier = rightDownstreamSupplier;
    }

    /**
     * Returns the bucket for the equal key of the given composite key, creating it if absent.
     * This is the single top-level lookup of an insert.
     */
    public JoinBucket<L, R> getOrCreateBucket(Object compositeKey) {
        var topKey = topEqualKeyUnpacker.apply(compositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var bucket = bucketMap.get(topKey);
        if (bucket == null) {
            bucket = new JoinBucket<>(leftDownstreamSupplier, rightDownstreamSupplier);
            bucketMap.put(topKey, bucket);
        }
        return bucket;
    }

    /**
     * @return the bucket for the equal key of the given composite key, or null if none exists.
     */
    public @Nullable JoinBucket<L, R> getBucket(Object compositeKey) {
        return bucketMap.get(topEqualKeyUnpacker.apply(compositeKey));
    }

    /**
     * Drops the bucket from the map iff both of its sides are empty.
     * A bucket holding a live tuple on either side is never reclaimed, so a cached reference to it
     * (kept on a tuple by the node) never goes stale.
     */
    public void removeBucketIfEmpty(Object compositeKey, JoinBucket<L, R> bucket) {
        if (bucket.isEmpty()) {
            bucketMap.remove(topEqualKeyUnpacker.apply(compositeKey));
        }
    }

    public boolean isEmpty() {
        return bucketMap.isEmpty();
    }

    @Override
    public String toString() {
        return "buckets = " + bucketMap.size();
    }

}
