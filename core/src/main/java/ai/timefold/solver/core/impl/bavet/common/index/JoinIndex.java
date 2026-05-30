package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    private final boolean hasSuffix;
    private final Supplier<Indexer<L>> leftDownstreamSupplier;
    private final Supplier<Indexer<R>> rightDownstreamSupplier;

    // Package-private: only IndexerFactory#buildJoinIndex (same package) creates a JoinIndex.
    JoinIndex(KeyUnpacker<Object> topEqualKeyUnpacker, boolean hasSuffix, Supplier<Indexer<L>> leftDownstreamSupplier,
            Supplier<Indexer<R>> rightDownstreamSupplier) {
        this.topEqualKeyUnpacker = topEqualKeyUnpacker;
        this.hasSuffix = hasSuffix;
        this.leftDownstreamSupplier = leftDownstreamSupplier;
        this.rightDownstreamSupplier = rightDownstreamSupplier;
    }

    /**
     * @return true when this index has a comparison/containing suffix below the equal prefix (so different
     *         composite keys can still share a bucket); false for a pure-equal index (the bucket key is the
     *         whole composite key). The node uses this to gate {@link #isSameBucket}.
     */
    public boolean hasSuffix() {
        return hasSuffix;
    }

    /**
     * Whether two composite keys resolve to the same bucket, i.e. share the same equal prefix. Used by the node
     * on a changed-key update to reuse the cached bucket (skipping a top-level lookup and a bucket drop/recreate)
     * when only the suffix changed.
     * <p>
     * Only meaningful for {@link #hasSuffix()} indexes (the node gates on it); for a pure-equal index the equal key
     * is the whole composite key, so this coincides with the full-key equality the caller has already ruled out.
     * {@code Objects.equals} because the equal-prefix component may be null (a nullable planning variable feeding
     * the equal joiner; the bucket map permits a null key).
     */
    public boolean isSameBucket(Object oldCompositeKey, Object newCompositeKey) {
        return Objects.equals(topEqualKeyUnpacker.apply(oldCompositeKey), topEqualKeyUnpacker.apply(newCompositeKey));
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
