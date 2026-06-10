package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A unified index for an equal-bearing join/ifExists node,
 * replacing the two parallel left/right {@link EqualIndexer}s
 * with ONE shared map from the equal-prefix key to a {@link Bucket}.
 * It assumes that all equal() joiners have been moved to the start of the query,
 * also called "equal-prefix".
 * <p>
 * An EQUAL match is {@code Objects.equals(leftKey, rightKey)}
 * (exactly how a {@link HashMap} groups entries)
 * so matching left and right tuples land in the SAME bucket;
 * co-location IS the match.
 * One top-level lookup per insert then replaces the second (cross-side) lookup the two-indexer path does.
 * <p>
 * The map is keyed by {@code topEqualKeyUnpacker.apply(compositeKey)}:
 * the whole composite key when the join is pure-equal (a single merged level),
 * or the key's first component when there is a comparison/containing suffix.
 * Callers pass the FULL composite key to every method;
 * the unpacking to the equal key happens here,
 * and the suffix levels are handled by the bucket's per-side downstream indexers.
 *
 * @param <L> the left element type (a left tuple, or {@code ExistsCounter} for ifExists)
 * @param <R> the right element type (a right {@code UniTuple})
 */
@NullMarked
public final class FusedEqualIndex<L, R> {

    // See EqualIndexer for the rationale behind the initial capacity and load factor.
    private final Map<Object, Bucket<L, R>> bucketMap = new HashMap<>(16, 0.5f);
    private final KeyUnpacker<Object> topEqualKeyUnpacker;
    private final boolean hasSuffix;
    private final Supplier<Indexer<L>> leftDownstreamSupplier;
    private final Supplier<Indexer<R>> rightDownstreamSupplier;

    // Package-private: only IndexerFactory#buildJoinIndex (same package) creates a JoinIndex.
    FusedEqualIndex(KeyUnpacker<Object> topEqualKeyUnpacker, boolean hasSuffix, Supplier<Indexer<L>> leftDownstreamSupplier,
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
    public Bucket<L, R> getOrCreateBucket(Object compositeKey) {
        var topKey = topEqualKeyUnpacker.apply(compositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var bucket = bucketMap.get(topKey);
        if (bucket == null) {
            bucket = new Bucket<>(leftDownstreamSupplier, rightDownstreamSupplier);
            bucketMap.put(topKey, bucket);
        }
        return bucket;
    }

    /**
     * @return the bucket for the equal key of the given composite key, or null if none exists.
     */
    public @Nullable Bucket<L, R> getBucket(Object compositeKey) {
        return bucketMap.get(topEqualKeyUnpacker.apply(compositeKey));
    }

    /**
     * Drops the bucket from the map iff both of its sides are empty.
     * A bucket holding a live tuple on either side is never reclaimed, so a cached reference to it
     * (kept on a tuple by the node) never goes stale.
     */
    public void removeBucketIfEmpty(Object compositeKey, Bucket<L, R> bucket) {
        if (bucket.isRemovable()) {
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

    /**
     * Co-locates the left and right tuples that share an equal-prefix key inside a {@link FusedEqualIndex}.
     * Holds one downstream {@link Indexer} per side:
     * for a pure-equal join each is simply a tuple-list {@link LinkedListLeafIndexer};
     * for an equal-prefix + comparison/containing join each is the per-side suffix sub-chain
     * (the right side built flipped, exactly as the two-indexer path's {@code indexerRight}).
     * <p>
     * The {@code compositeKey} passed to the per-side methods is the FULL composite key;
     * a backend downstream ignores it, a suffix sub-chain extracts its levels from it
     * (via {@code CompositeKeyUnpacker(1)}, ...).
     * The left downstream holds left tuples and is queried with a right tuple's key;
     * the right downstream holds right tuples and is queried with a left tuple's key
     * (mirroring the two-indexer cross-side query).
     *
     * @param <L> the left element type (a left tuple, or {@code ExistsCounter} for ifExists)
     * @param <R> the right element type (a right {@code UniTuple})
     */
    @NullMarked
    public record Bucket<L, R>(Indexer<L> leftDownstream, Indexer<R> rightDownstream) {

        // Package-private: only JoinIndex (same package) creates buckets.
        public Bucket(Supplier<Indexer<L>> leftDownstream, Supplier<Indexer<R>> rightDownstream) {
            this(leftDownstream.get(), rightDownstream.get());
        }

        /**
         * @return true when both sides are removable (empty), so the bucket can be dropped from the
         *         {@link FusedEqualIndex}. A bucket holding a live tuple on either side is never reclaimed.
         */
        boolean isRemovable() {
            return leftDownstream.isRemovable() && rightDownstream.isRemovable();
        }

    }

}
