package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.index.FusedEqualIndex;
import ai.timefold.solver.core.impl.bavet.common.index.FusedEqualIndex.Bucket;
import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.Nullable;

/**
 * There is a strong likelihood that any change to this class, which is not related to indexing,
 * should also be made to {@link AbstractUnindexedJoinNode}.
 * <p>
 * Indexing takes one of two forms, chosen once at construction (see {@link IndexerFactory#isFusedEqualIndexEligible()}):
 * <ul>
 * <li>the non-unified path keeps two parallel {@link Indexer}s ({@code indexerLeft}/{@code indexerRight}); a tuple
 * inserted on one side queries the OPPOSITE indexer with its OWN key (a second hash navigation of that key);</li>
 * <li>the unified path (equal-bearing joins) keeps ONE {@link FusedEqualIndex}: a tuple looks up its bucket ONCE, adds
 * itself to its side, and iterates the other side of the SAME bucket — co-location is the equal match. The resolved
 * bucket is cached on the tuple so same-key updates and retracts need no lookup at all.</li>
 * </ul>
 * The out-tuple/propagation logic in {@link AbstractJoinNode} is identical for both.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedJoinNode<LeftTuple_ extends Tuple, Right_, OutTuple_ extends Tuple>
        extends AbstractJoinNode<LeftTuple_, Right_, OutTuple_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final KeysExtractor<LeftTuple_> keysExtractorLeft;
    private final UniKeysExtractor<Right_> keysExtractorRight;
    private final int inputStoreIndexLeftCompositeKey;
    private final int inputStoreIndexLeftEntry;
    private final int inputStoreIndexRightCompositeKey;
    private final int inputStoreIndexRightEntry;
    private final boolean useFusedEqualIndex;
    // Non-unified path (useFusedEqualIndex == false): two parallel indexers, queried cross-side.
    private final @Nullable Indexer<LeftTuple_> indexerLeft;
    private final @Nullable Indexer<UniTuple<Right_>> indexerRight;
    // Unified path (useFusedEqualIndex == true): one shared join index, plus per-side cached-bucket store slots.
    private final @Nullable FusedEqualIndex<LeftTuple_, UniTuple<Right_>> fusedEqualIndex;
    private final int inputStoreIndexLeftBucket;
    private final int inputStoreIndexRightBucket;
    // True only for an equal+suffix unified index: a changed-key update whose equal prefix is unchanged can reuse the
    // cached bucket. Pure-equal nodes are false, so the dominant path never even evaluates isSameBucket.
    private final boolean reuseBucketEligible;

    protected AbstractIndexedJoinNode(KeysExtractor<LeftTuple_> keysExtractorLeft, IndexerFactory<Right_> indexerFactory,
            TupleLifecycle<OutTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InOutTupleStorePositionTracker tupleStorePositionTracker) {
        super(nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLeftCompositeKey = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexLeftEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightCompositeKey = tupleStorePositionTracker.reserveNextRight();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
        this.useFusedEqualIndex = indexerFactory.isFusedEqualIndexEligible();
        this.fusedEqualIndex = useFusedEqualIndex ? indexerFactory.buildFusedEqualIndex() : null;
        this.indexerLeft = useFusedEqualIndex ? null : indexerFactory.buildIndexer(true);
        this.indexerRight = useFusedEqualIndex ? null : indexerFactory.buildIndexer(false);
        this.inputStoreIndexLeftBucket = useFusedEqualIndex ? tupleStorePositionTracker.reserveNextLeft() : -1;
        this.inputStoreIndexRightBucket = useFusedEqualIndex ? tupleStorePositionTracker.reserveNextRight() : -1;
        this.reuseBucketEligible = useFusedEqualIndex && fusedEqualIndex.hasSuffix();
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLeftCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(leftTuple));
        }
        var compositeKey = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLeftOutTupleList, new ElementAwareLinkedList<OutTuple_>());
        indexAndPropagateLeft(leftTuple, compositeKey, false);
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        var oldCompositeKey = leftTuple.getStore(inputStoreIndexLeftCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var newCompositeKey = keysExtractorLeft.apply(leftTuple);
        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            // Prefer an update over retract-insert if possible
            innerUpdateLeft(leftTuple, consumer -> forEachRightMatch(leftTuple, oldCompositeKey, consumer));
        } else {
            ElementAwareLinkedList<OutTuple_> outTupleListLeft = leftTuple.getStore(inputStoreIndexLeftOutTupleList);
            var reuseBucket = reuseBucketEligible && fusedEqualIndex.isSameBucket(oldCompositeKey, newCompositeKey);
            if (reuseBucket) {
                // Equal prefix unchanged ⇒ same bucket: move within the cached bucket, no top lookup or drop/recreate.
                Bucket<LeftTuple_, UniTuple<Right_>> bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
                bucket.leftDownstream().remove(oldCompositeKey, leftTuple.getStore(inputStoreIndexLeftEntry));
            } else {
                ListEntry<LeftTuple_> entry = leftTuple.getStore(inputStoreIndexLeftEntry);
                if (useFusedEqualIndex) {
                    Bucket<LeftTuple_, UniTuple<Right_>> oldBucket = leftTuple.getStore(inputStoreIndexLeftBucket);
                    oldBucket.leftDownstream().remove(oldCompositeKey, entry);
                    fusedEqualIndex.removeBucketIfEmpty(oldCompositeKey, oldBucket);
                } else {
                    indexerLeft.remove(oldCompositeKey, entry);
                }
            }
            outTupleListLeft.clear(this::retractOutTupleByLeft);
            // outTupleListLeft is now empty
            // No need for leftTuple.setStore(inputStoreIndexLeftOutTupleList, outTupleListLeft);
            indexAndPropagateLeft(leftTuple, newCompositeKey, reuseBucket);
        }
    }

    private void indexAndPropagateLeft(LeftTuple_ leftTuple, Object compositeKey, boolean reuseCachedBucket) {
        leftTuple.setStore(inputStoreIndexLeftCompositeKey, compositeKey);
        if (useFusedEqualIndex) {
            // reuseCachedBucket: the equal prefix is unchanged, so the cached bucket is still correct — no top-level
            // lookup and no re-cache; otherwise resolve the bucket (the single top-level lookup) and cache it.
            Bucket<LeftTuple_, UniTuple<Right_>> bucket;
            if (reuseCachedBucket) {
                bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
            } else {
                bucket = fusedEqualIndex.getOrCreateBucket(compositeKey);
                leftTuple.setStore(inputStoreIndexLeftBucket, bucket);
            }
            leftTuple.setStore(inputStoreIndexLeftEntry, bucket.leftDownstream().put(compositeKey, leftTuple));
        } else {
            leftTuple.setStore(inputStoreIndexLeftEntry, indexerLeft.put(compositeKey, leftTuple));
        }
        if (!leftTuple.getState().isActive()) {
            // Assume the following scenario:
            // - The join is of two entities of the same type, both filtering out unassigned.
            // - One entity became unassigned, so the outTuple is getting retracted.
            // - The other entity became assigned, and is therefore getting inserted.
            //
            // This means the filter would be called with (unassignedEntity, assignedEntity),
            // which breaks the expectation that the filter is only called on two assigned entities
            // and requires adding null checks to the filter for something that should intuitively be impossible.
            // We avoid this situation as it is clear that it is pointless to insert this tuple.
            //
            // It is possible that the same problem would exist coming from the other side as well,
            // and therefore the right tuple would have to be checked for active state as well.
            // However, no such issue could have been reproduced; when in doubt, leave it out.
            return;
        }
        forEachRightMatch(leftTuple, compositeKey, rightTuple -> insertOutTupleFiltered(leftTuple, rightTuple));
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var compositeKey = leftTuple.removeStore(inputStoreIndexLeftCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListLeft = leftTuple.removeStore(inputStoreIndexLeftOutTupleList);
        ListEntry<LeftTuple_> entry = leftTuple.removeStore(inputStoreIndexLeftEntry);
        if (useFusedEqualIndex) {
            Bucket<LeftTuple_, UniTuple<Right_>> bucket = leftTuple.removeStore(inputStoreIndexLeftBucket);
            bucket.leftDownstream().remove(compositeKey, entry);
            fusedEqualIndex.removeBucketIfEmpty(compositeKey, bucket);
        } else {
            indexerLeft.remove(compositeKey, entry);
        }
        outTupleListLeft.clear(this::retractOutTupleByLeft);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var compositeKey = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightOutTupleList, new ElementAwareLinkedList<OutTuple_>());
        indexAndPropagateRight(rightTuple, compositeKey, false);
    }

    @Override
    public final void updateRight(UniTuple<Right_> rightTuple) {
        var oldCompositeKey = rightTuple.getStore(inputStoreIndexRightCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertRight(rightTuple);
            return;
        }
        var newCompositeKey = keysExtractorRight.apply(rightTuple);
        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            // Prefer an update over retract-insert if possible
            innerUpdateRight(rightTuple, consumer -> forEachLeftMatch(rightTuple, oldCompositeKey, consumer));
        } else {
            ElementAwareLinkedList<OutTuple_> outTupleListRight = rightTuple.getStore(inputStoreIndexRightOutTupleList);
            var reuseBucket = reuseBucketEligible && fusedEqualIndex.isSameBucket(oldCompositeKey, newCompositeKey);
            if (reuseBucket) {
                // Equal prefix unchanged ⇒ same bucket: move within the cached bucket, no top lookup or drop/recreate.
                Bucket<LeftTuple_, UniTuple<Right_>> bucket = rightTuple.getStore(inputStoreIndexRightBucket);
                bucket.rightDownstream().remove(oldCompositeKey, rightTuple.getStore(inputStoreIndexRightEntry));
            } else {
                ListEntry<UniTuple<Right_>> entry = rightTuple.getStore(inputStoreIndexRightEntry);
                if (useFusedEqualIndex) {
                    Bucket<LeftTuple_, UniTuple<Right_>> bucket = rightTuple.getStore(inputStoreIndexRightBucket);
                    bucket.rightDownstream().remove(oldCompositeKey, entry);
                    fusedEqualIndex.removeBucketIfEmpty(oldCompositeKey, bucket);
                } else {
                    indexerRight.remove(oldCompositeKey, entry);
                }
            }
            outTupleListRight.clear(this::retractOutTupleByRight);
            // outTupleListRight is now empty
            // No need for rightTuple.setStore(inputStoreIndexRightOutTupleList, outTupleListRight);
            indexAndPropagateRight(rightTuple, newCompositeKey, reuseBucket);
        }
    }

    private void indexAndPropagateRight(UniTuple<Right_> rightTuple, Object compositeKey, boolean reuseCachedBucket) {
        rightTuple.setStore(inputStoreIndexRightCompositeKey, compositeKey);
        if (useFusedEqualIndex) {
            // reuseCachedBucket: the equal prefix is unchanged, so the cached bucket is still correct — no top-level
            // lookup and no re-cache; otherwise resolve the bucket (the single top-level lookup) and cache it.
            Bucket<LeftTuple_, UniTuple<Right_>> bucket;
            if (reuseCachedBucket) {
                bucket = rightTuple.getStore(inputStoreIndexRightBucket);
            } else {
                bucket = fusedEqualIndex.getOrCreateBucket(compositeKey);
                rightTuple.setStore(inputStoreIndexRightBucket, bucket);
            }
            rightTuple.setStore(inputStoreIndexRightEntry, bucket.rightDownstream().put(compositeKey, rightTuple));
        } else {
            rightTuple.setStore(inputStoreIndexRightEntry, indexerRight.put(compositeKey, rightTuple));
        }
        forEachLeftMatch(rightTuple, compositeKey, leftTuple -> insertOutTupleFilteredFromLeft(leftTuple, rightTuple));
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var compositeKey = rightTuple.removeStore(inputStoreIndexRightCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ElementAwareLinkedList<OutTuple_> outTupleListRight = rightTuple.removeStore(inputStoreIndexRightOutTupleList);
        ListEntry<UniTuple<Right_>> entry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (useFusedEqualIndex) {
            Bucket<LeftTuple_, UniTuple<Right_>> bucket = rightTuple.removeStore(inputStoreIndexRightBucket);
            bucket.rightDownstream().remove(compositeKey, entry);
            fusedEqualIndex.removeBucketIfEmpty(compositeKey, bucket);
        } else {
            indexerRight.remove(compositeKey, entry);
        }

        outTupleListRight.clear(this::retractOutTupleByRight);
    }

    /**
     * Iterates the right tuples matching the given left composite key: the right side of the left tuple's cached
     * bucket (unified), or {@code indexerRight} queried with that key (non-unified).
     */
    private void forEachRightMatch(LeftTuple_ leftTuple, Object compositeKey, Consumer<UniTuple<Right_>> consumer) {
        if (useFusedEqualIndex) {
            Bucket<LeftTuple_, UniTuple<Right_>> bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
            bucket.rightDownstream().forEach(compositeKey, consumer);
        } else {
            indexerRight.forEach(compositeKey, consumer);
        }
    }

    /**
     * Iterates the left tuples matching the given right composite key: the left side of the right tuple's cached
     * bucket (unified), or {@code indexerLeft} queried with that key (non-unified).
     */
    private void forEachLeftMatch(UniTuple<Right_> rightTuple, Object compositeKey, Consumer<LeftTuple_> consumer) {
        if (useFusedEqualIndex) {
            Bucket<LeftTuple_, UniTuple<Right_>> bucket = rightTuple.getStore(inputStoreIndexRightBucket);
            bucket.leftDownstream().forEach(compositeKey, consumer);
        } else {
            indexerLeft.forEach(compositeKey, consumer);
        }
    }

}
