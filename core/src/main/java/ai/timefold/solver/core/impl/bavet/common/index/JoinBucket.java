package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * Co-locates the left and right tuples that share an equal-prefix key inside a {@link JoinIndex}.
 * Holds one downstream {@link Indexer} per side: for a pure-equal join each is simply a tuple-list
 * {@link LinkedListIndexerBackend}; for an equal-prefix + comparison/containing join each is the per-side
 * suffix sub-chain (the right side built flipped, exactly as the two-indexer path's {@code indexerRight}).
 * <p>
 * The {@code compositeKey} passed to the per-side methods is the FULL composite key; a backend downstream
 * ignores it, a suffix sub-chain extracts its levels from it (via {@code CompositeKeyUnpacker(1)}, ...).
 * The left downstream holds left tuples and is queried with a right tuple's key; the right downstream holds
 * right tuples and is queried with a left tuple's key — mirroring the two-indexer cross-side query.
 *
 * @param <L> the left element type (a left tuple, or {@code ExistsCounter} for ifExists)
 * @param <R> the right element type (a right {@code UniTuple})
 */
@NullMarked
public final class JoinBucket<L, R> {

    private final Indexer<L> leftDownstream;
    private final Indexer<R> rightDownstream;

    // Package-private: only JoinIndex (same package) creates buckets.
    JoinBucket(Supplier<Indexer<L>> leftDownstreamSupplier, Supplier<Indexer<R>> rightDownstreamSupplier) {
        this.leftDownstream = leftDownstreamSupplier.get();
        this.rightDownstream = rightDownstreamSupplier.get();
    }

    public ListEntry<L> addLeft(Object compositeKey, L tuple) {
        return leftDownstream.put(compositeKey, tuple);
    }

    public ListEntry<R> addRight(Object compositeKey, R tuple) {
        return rightDownstream.put(compositeKey, tuple);
    }

    public void removeLeft(Object compositeKey, ListEntry<L> entry) {
        leftDownstream.remove(compositeKey, entry);
    }

    public void removeRight(Object compositeKey, ListEntry<R> entry) {
        rightDownstream.remove(compositeKey, entry);
    }

    public void forEachLeft(Object compositeKey, Consumer<L> tupleConsumer) {
        leftDownstream.forEach(compositeKey, tupleConsumer);
    }

    public void forEachRight(Object compositeKey, Consumer<R> tupleConsumer) {
        rightDownstream.forEach(compositeKey, tupleConsumer);
    }

    public int rightSize(Object compositeKey) {
        return rightDownstream.size(compositeKey);
    }

    /**
     * @return true when both sides are removable (empty), so the bucket can be dropped from the
     *         {@link JoinIndex}. A bucket holding a live tuple on either side is never reclaimed.
     */
    public boolean isEmpty() {
        return leftDownstream.isRemovable() && rightDownstream.isRemovable();
    }

    @Override
    public String toString() {
        return "left (%s), right (%s)".formatted(leftDownstream, rightDownstream);
    }

}
