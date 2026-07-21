package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.bavet.common.index.FusedEqualIndex;
import ai.timefold.solver.core.impl.bavet.common.index.FusedEqualIndex.Bucket;
import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.KeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory.UniKeysExtractor;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.indictment.IndictmentSource;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.Nullable;

/**
 * There is a strong likelihood that any change to this class, which is not related to indexing,
 * should also be made to {@link AbstractUnindexedIfExistsNode}.
 * <p>
 * Indexing takes one of two forms, chosen once at construction (see {@link IndexerFactory#isFusedEqualIndexEligible()}).
 * The non-unified path keeps two parallel {@link Indexer}s; the unified path (equal-bearing) keeps one
 * {@link FusedEqualIndex} whose buckets co-locate the left counters and the right tuples sharing an equal key, with the
 * resolved bucket cached on the tuple so same-key updates and retracts need no lookup. The counter / filtering-tracker
 * logic in {@link AbstractIfExistsNode} is identical for both.
 *
 * @param <LeftTuple_>
 * @param <Right_>
 */
public abstract class AbstractIndexedIfExistsNode<LeftTuple_ extends Tuple, Right_>
        extends AbstractIfExistsNode<LeftTuple_, Right_>
        implements LeftTupleLifecycle<LeftTuple_>, RightTupleLifecycle<UniTuple<Right_>> {

    private final KeysExtractor<LeftTuple_> keysExtractorLeft;
    private final UniKeysExtractor<Right_> keysExtractorRight;
    private final int inputStoreIndexLefCompositeKey;
    private final int inputStoreIndexLeftCounterEntry;
    private final int inputStoreIndexRightCompositeKey;
    private final int inputStoreIndexRightEntry;
    private final boolean useFusedEqualIndex;
    // Non-unified path (useFusedEqualIndex == false): two parallel indexers, queried cross-side.
    private final @Nullable Indexer<ExistsCounter<LeftTuple_>> indexerLeft;
    private final @Nullable Indexer<UniTuple<Right_>> indexerRight;
    // Unified path (useFusedEqualIndex == true): one shared join index, plus per-side cached-bucket store slots.
    private final @Nullable FusedEqualIndex<ExistsCounter<LeftTuple_>, UniTuple<Right_>> fusedEqualIndex;
    private final int inputStoreIndexLeftBucket;
    private final int inputStoreIndexRightBucket;
    // True only for an equal+suffix unified index: a changed-key update whose equal prefix is unchanged can reuse the
    // cached bucket. Pure-equal nodes are false, so the dominant path never even evaluates isSameBucket.
    private final boolean reuseBucketEligible;

    protected AbstractIndexedIfExistsNode(boolean shouldExist, KeysExtractor<LeftTuple_> keysExtractorLeft,
            IndexerFactory<Right_> indexerFactory, TupleLifecycle<LeftTuple_> nextNodesTupleLifecycle, boolean isFiltering,
            InTupleStorePositionTracker tupleStorePositionTracker) {
        super(shouldExist, nextNodesTupleLifecycle, isFiltering, tupleStorePositionTracker);
        this.keysExtractorLeft = keysExtractorLeft;
        this.keysExtractorRight = indexerFactory.buildRightKeysExtractor();
        this.inputStoreIndexLefCompositeKey = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexLeftCounterEntry = tupleStorePositionTracker.reserveNextLeft();
        this.inputStoreIndexRightCompositeKey = tupleStorePositionTracker.reserveNextRight();
        this.inputStoreIndexRightEntry = tupleStorePositionTracker.reserveNextRight();
        this.useFusedEqualIndex = indexerFactory.isFusedEqualIndexEligible();
        if (useFusedEqualIndex) {
            this.fusedEqualIndex = indexerFactory.buildFusedEqualIndex();
            this.indexerLeft = null;
            this.indexerRight = null;
            this.inputStoreIndexLeftBucket = tupleStorePositionTracker.reserveNextLeft();
            this.inputStoreIndexRightBucket = tupleStorePositionTracker.reserveNextRight();
        } else {
            this.fusedEqualIndex = null;
            this.indexerLeft = indexerFactory.buildIndexer(true);
            this.indexerRight = indexerFactory.buildIndexer(false);
            this.inputStoreIndexLeftBucket = -1;
            this.inputStoreIndexRightBucket = -1;
        }
        this.reuseBucketEligible = useFusedEqualIndex && fusedEqualIndex.hasSuffix();
    }

    @Override
    public final void insertLeft(LeftTuple_ leftTuple) {
        if (leftTuple.getStore(inputStoreIndexLefCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore.".formatted(leftTuple));
        }
        var compositeKey = keysExtractorLeft.apply(leftTuple);
        leftTuple.setStore(inputStoreIndexLefCompositeKey, compositeKey);

        var counter = new ExistsCounter<>(leftTuple);
        updateCounterRight(leftTuple, compositeKey, counter, putLeftCounter(leftTuple, compositeKey, counter, false));
        initCounterLeft(counter);
    }

    private void updateCounterRight(LeftTuple_ leftTuple, Object compositeKey, ExistsCounter<LeftTuple_> counter,
            ListEntry<ExistsCounter<LeftTuple_>> counterEntry) {
        leftTuple.setStore(inputStoreIndexLeftCounterEntry, counterEntry);
        if (!isFiltering) {
            counter.countRight = rightSize(leftTuple, compositeKey);
        } else {
            // Trackers link themselves into the left tuple's inputStoreIndexLeftTrackerList slot.
            // No list object is needed; the slot starts null and the first tracker becomes the head.
            forEachRightFromLeft(leftTuple, compositeKey,
                    rightTuple -> updateCounterFromLeft(counter, rightTuple));
        }
    }

    private int rightSize(LeftTuple_ leftTuple, Object compositeKey) {
        if (useFusedEqualIndex) {
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
            return bucket.sizeRight(compositeKey);
        } else {
            return indexerRight.size(compositeKey);
        }
    }

    @Override
    public final void updateLeft(LeftTuple_ leftTuple) {
        var oldCompositeKey = leftTuple.getStore(inputStoreIndexLefCompositeKey);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insertLeft(leftTuple);
            return;
        }
        var newCompositeKey = keysExtractorLeft.apply(leftTuple);
        ListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.getStore(inputStoreIndexLeftCounterEntry);
        var counter = counterEntry.element();

        if (oldCompositeKey.equals(newCompositeKey)) {
            // No need for re-indexing because the index keys didn't change
            // The indexers contain counters in the DEAD state, to track the rightCount.
            if (!isFiltering) {
                updateUnchangedCounterLeft(counter);
            } else {
                // Call filtering for the leftTuple and rightTuple combinations again
                clearLeftTrackerList(leftTuple);
                counter.countRight = 0;
                forEachRightFromLeft(leftTuple, oldCompositeKey,
                        rightTuple -> updateCounterFromLeft(counter, rightTuple));
                updateCounterLeft(counter);
            }
        } else {
            // sameBucket: equal prefix unchanged ⇒ keep & reuse the cached bucket (no top lookup, no drop/recreate).
            var sameBucket = reuseBucketEligible && fusedEqualIndex.isSameBucket(oldCompositeKey, newCompositeKey);
            updateIndexerLeft(oldCompositeKey, counterEntry, leftTuple, sameBucket);
            counter.countRight = 0;
            leftTuple.setStore(inputStoreIndexLefCompositeKey, newCompositeKey);
            updateCounterRight(leftTuple, newCompositeKey, counter,
                    putLeftCounter(leftTuple, newCompositeKey, counter, sameBucket));
            updateCounterLeft(counter);
        }
    }

    @Override
    public final void retractLeft(LeftTuple_ leftTuple) {
        var compositeKey = leftTuple.removeStore(inputStoreIndexLefCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ListEntry<ExistsCounter<LeftTuple_>> counterEntry = leftTuple.removeStore(inputStoreIndexLeftCounterEntry);
        var element = counterEntry.element(); // Store so that the reference survives removal.
        removeFromIndexerLeft(compositeKey, leftTuple, counterEntry, false);
        clearLeftTrackerList(leftTuple);
        killCounterLeft(element);
    }

    private void removeFromIndexerLeft(Object compositeKey, LeftTuple_ leftTuple,
            ListEntry<ExistsCounter<LeftTuple_>> counterEntry, boolean keepBucket) {
        if (useFusedEqualIndex) {
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket =
                    keepBucket ? leftTuple.getStore(inputStoreIndexLeftBucket)
                            : leftTuple.removeStore(inputStoreIndexLeftBucket);
            bucket.removeLeft(compositeKey, counterEntry);
            if (!keepBucket) { // keepBucket: a same-bucket changed-key update re-adds immediately, so don't drop it.
                fusedEqualIndex.removeBucketIfEmpty(compositeKey, bucket);
            }
        } else {
            indexerLeft.remove(compositeKey, counterEntry);
        }
    }

    private void updateIndexerLeft(Object compositeKey, ListEntry<ExistsCounter<LeftTuple_>> counterEntry, LeftTuple_ leftTuple,
            boolean keepBucket) {
        removeFromIndexerLeft(compositeKey, leftTuple, counterEntry, keepBucket);
        clearLeftTrackerList(leftTuple);
    }

    @Override
    public final void insertRight(UniTuple<Right_> rightTuple) {
        if (rightTuple.getStore(inputStoreIndexRightCompositeKey) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(rightTuple));
        }
        var compositeKey = keysExtractorRight.apply(rightTuple);
        rightTuple.setStore(inputStoreIndexRightCompositeKey, compositeKey);
        rightTuple.setStore(inputStoreIndexRightEntry, putRightTuple(rightTuple, compositeKey, false));
        updateCounterLeft(rightTuple, compositeKey);
    }

    private void updateCounterLeft(UniTuple<Right_> rightTuple, Object compositeKey) {
        if (!isFiltering) {
            // To prevent creating a dynamic lambda on the hot path,
            // only call the 2-args version when indictments are enabled
            if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                forEachLeftCounter(rightTuple, compositeKey, this::incrementCounterRightWithoutIndictment);
            } else {
                forEachLeftCounter(rightTuple, compositeKey,
                        counter -> incrementCounterRightUpdatingIndictment(counter, rightTuple));
            }
        } else {
            // Trackers link themselves into the right tuple's inputStoreIndexRightTrackerList slot.
            // No list object is needed; the slot starts null and the first tracker becomes the head.
            forEachLeftCounter(rightTuple, compositeKey,
                    counter -> updateCounterFromRight(counter, rightTuple));
        }
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
            if (isFiltering) {
                clearRightTrackerList(rightTuple);
                forEachLeftCounter(rightTuple, oldCompositeKey,
                        counter -> updateCounterFromRight(counter, rightTuple));
            }
        } else {
            // sameBucket: equal prefix unchanged ⇒ keep & reuse the cached bucket (no top lookup, no drop/recreate).
            var sameBucket = reuseBucketEligible && fusedEqualIndex.isSameBucket(oldCompositeKey, newCompositeKey);
            ListEntry<UniTuple<Right_>> entry = rightTuple.getStore(inputStoreIndexRightEntry);
            if (useFusedEqualIndex) {
                Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket = rightTuple.getStore(inputStoreIndexRightBucket);
                bucket.removeRight(oldCompositeKey, entry);
                if (!sameBucket) { // keepBucket: a same-bucket changed-key update re-adds immediately, so don't drop it.
                    fusedEqualIndex.removeBucketIfEmpty(oldCompositeKey, bucket);
                }
            } else {
                indexerRight.remove(oldCompositeKey, entry);
            }
            if (!isFiltering) {
                // To prevent creating a dynamic lambda on the hot path,
                // only call the 2-args version when indictments are enabled
                if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                    forEachLeftCounter(rightTuple, oldCompositeKey, this::decrementCounterRightWithoutIndictment);
                } else {
                    forEachLeftCounter(rightTuple, oldCompositeKey,
                            counter -> decrementCounterRightUpdatingIndictment(counter, rightTuple));
                }
            } else {
                clearRightTrackerList(rightTuple);
            }
            rightTuple.setStore(inputStoreIndexRightCompositeKey, newCompositeKey);
            rightTuple.setStore(inputStoreIndexRightEntry, putRightTuple(rightTuple, newCompositeKey, sameBucket));
            updateCounterLeft(rightTuple, newCompositeKey);
        }
    }

    @Override
    public final void retractRight(UniTuple<Right_> rightTuple) {
        var compositeKey = rightTuple.removeStore(inputStoreIndexRightCompositeKey);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        ListEntry<UniTuple<Right_>> entry = rightTuple.removeStore(inputStoreIndexRightEntry);
        if (useFusedEqualIndex) {
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket = rightTuple.removeStore(inputStoreIndexRightBucket);
            bucket.removeRight(compositeKey, entry);
            fusedEqualIndex.removeBucketIfEmpty(compositeKey, bucket);
            if (!isFiltering) {
                // To prevent creating a dynamic lambda on the hot path,
                // only call the 2-args version when indictments are enabled
                if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                    bucket.forEachLeft(compositeKey, this::decrementCounterRightWithoutIndictment);
                } else {
                    bucket.forEachLeft(compositeKey, counter -> decrementCounterRightUpdatingIndictment(counter, rightTuple));
                }
            } else {
                clearRightTrackerList(rightTuple);
            }
        } else {
            indexerRight.remove(compositeKey, entry);
            if (!isFiltering) {
                // To prevent creating a dynamic lambda on the hot path,
                // only call the 2-args version when indictments are enabled
                if (rightTuple.getIndictmentSource() == IndictmentSource.DISABLED) {
                    indexerLeft.forEach(compositeKey, this::decrementCounterRightWithoutIndictment);
                } else {
                    indexerLeft.forEach(compositeKey, counter -> decrementCounterRightUpdatingIndictment(counter, rightTuple));
                }
            } else {
                clearRightTrackerList(rightTuple);
            }
        }
    }

    /**
     * Adds a left counter to its side of the index, caching the resolved bucket on the left tuple (unified path).
     */
    private ListEntry<ExistsCounter<LeftTuple_>> putLeftCounter(LeftTuple_ leftTuple, Object compositeKey,
            ExistsCounter<LeftTuple_> counter, boolean reuseCachedBucket) {
        if (useFusedEqualIndex) {
            // reuseCachedBucket: the equal prefix is unchanged, so the cached bucket is still correct — no top-level
            // lookup and no re-cache; otherwise resolve the bucket (the single top-level lookup) and cache it.
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket;
            if (reuseCachedBucket) {
                bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
            } else {
                bucket = fusedEqualIndex.getOrCreateBucket(compositeKey);
                leftTuple.setStore(inputStoreIndexLeftBucket, bucket);
            }
            return bucket.putLeft(compositeKey, counter);
        } else {
            return indexerLeft.put(compositeKey, counter);
        }
    }

    /**
     * Adds a right tuple to its side of the index, caching the resolved bucket on the right tuple (unified path).
     */
    private ListEntry<UniTuple<Right_>> putRightTuple(UniTuple<Right_> rightTuple, Object compositeKey,
            boolean reuseCachedBucket) {
        if (useFusedEqualIndex) {
            // reuseCachedBucket: the equal prefix is unchanged, so the cached bucket is still correct — no top-level
            // lookup and no re-cache; otherwise resolve the bucket (the single top-level lookup) and cache it.
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket;
            if (reuseCachedBucket) {
                bucket = rightTuple.getStore(inputStoreIndexRightBucket);
            } else {
                bucket = fusedEqualIndex.getOrCreateBucket(compositeKey);
                rightTuple.setStore(inputStoreIndexRightBucket, bucket);
            }
            return bucket.putRight(compositeKey, rightTuple);
        } else {
            return indexerRight.put(compositeKey, rightTuple);
        }
    }

    /** Iterates the right tuples matching a left counter's composite key (its cached bucket, or {@code indexerRight}). */
    private void forEachRightFromLeft(LeftTuple_ leftTuple, Object compositeKey, Consumer<UniTuple<Right_>> consumer) {
        if (useFusedEqualIndex) {
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket = leftTuple.getStore(inputStoreIndexLeftBucket);
            bucket.forEachRight(compositeKey, consumer);
        } else {
            indexerRight.forEach(compositeKey, consumer);
        }
    }

    /** Iterates the left counters matching a right tuple's composite key (its cached bucket, or {@code indexerLeft}). */
    private void forEachLeftCounter(UniTuple<Right_> rightTuple, Object compositeKey,
            Consumer<ExistsCounter<LeftTuple_>> consumer) {
        if (useFusedEqualIndex) {
            Bucket<ExistsCounter<LeftTuple_>, UniTuple<Right_>> bucket = rightTuple.getStore(inputStoreIndexRightBucket);
            bucket.forEachLeft(compositeKey, consumer);
        } else {
            indexerLeft.forEach(compositeKey, consumer);
        }
    }

}
