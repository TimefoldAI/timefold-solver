package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;

/**
 * The implementation records the tuples each object affects inside
 * an internal {@link AbstractBavetNodeNetwork} and replays them on update.
 * Used by {@link AbstractPrecomputeNode} to precompute constraint streams.
 *
 * @param <Tuple_>
 */
@NullMarked
public final class RecordAndReplayPropagator<Tuple_ extends Tuple>
        implements Propagator {

    private final Set<Object> retractQueue;
    private final Set<Object> insertQueue;

    // Store entities and facts separately; we don't need to precompute
    // the tuples for facts, since facts never update
    private final Set<Object> seenEntitySet;
    private final Set<Object> seenFactSet;

    private final Supplier<BavetPrecomputeBuildHelper<Tuple_>> precomputeBuildHelperSupplier;
    private final UnaryOperator<Tuple_> internalTupleToOutputTupleMapper;
    private final Map<Object, List<Tuple_>> objectToOutputTuplesMap;
    /**
     * Output tuples which depend only on problem facts.
     * Unlike entity-derived output, these are not stored per-source
     * (facts never update, so they need no replay),
     * but they must still be delivered downstream exactly once.
     * Retained between recalculations so they can be retracted on cache invalidation.
     */
    private final List<Tuple_> factOutputTupleList = new ArrayList<>();
    private final Set<Object> alreadyUpdatingSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Class<?>, Boolean> objectClassToIsEntitySourceClassMap;

    private final StaticPropagationQueue<Tuple_> propagationQueue;

    public RecordAndReplayPropagator(Supplier<BavetPrecomputeBuildHelper<Tuple_>> precomputeBuildHelperSupplier,
            UnaryOperator<Tuple_> internalTupleToOutputTupleMapper, TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        this.precomputeBuildHelperSupplier = precomputeBuildHelperSupplier;
        this.internalTupleToOutputTupleMapper = internalTupleToOutputTupleMapper;
        this.objectToOutputTuplesMap = new IdentityHashMap<>(size);

        // Guesstimate that updates are dominant.
        this.retractQueue = CollectionUtils.newIdentityHashSet(size / 20);
        this.insertQueue = CollectionUtils.newIdentityHashSet(size / 20);
        this.objectClassToIsEntitySourceClassMap = new HashMap<>();
        this.seenEntitySet = CollectionUtils.newIdentityHashSet(size);
        this.seenFactSet = CollectionUtils.newIdentityHashSet(size);

        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    public RecordAndReplayPropagator(Supplier<BavetPrecomputeBuildHelper<Tuple_>> precomputeBuildHelperSupplier,
            UnaryOperator<Tuple_> internalTupleToOutputTupleMapper, TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(precomputeBuildHelperSupplier, internalTupleToOutputTupleMapper, nextNodesTupleLifecycle, 1000);
    }

    public boolean canProduceTuples() {
        // This is correct, but not optimal.
        // These conditions guarantee that deactivation will only happen when safe,
        // but it will not deactivate in all cases;
        // for that, the activity check would have to happen after all inserts, updates and retracts were propagated once.
        return !objectToOutputTuplesMap.isEmpty() // Tuples were produced.
                || !insertQueue.isEmpty(); // Tuples will be produced, unless retract removes them from the insert queue.
    }

    public void insert(Object object) {
        // do not remove a retract of the same fact (a fact was updated)
        insertQueue.add(object);
    }

    public void update(Object object) {
        if (!alreadyUpdatingSet.add(object)) {
            // The list was already sent to the propagation queue.
            // Don't iterate over it again, even though the queue would deduplicate its contents.
            return;
        }
        // Updates happen very frequently, so we optimize by avoiding the update queue
        // and going straight to the propagation queue.
        // The propagation queue deduplicates updates internally.
        var outTupleList = objectToOutputTuplesMap.get(object);
        if (outTupleList != null) {
            outTupleList.forEach(propagationQueue::update);
        }
    }

    public void retract(Object object) {
        // remove an insert then retract (a fact was inserted but retracted before settling)
        // do not remove a retract then insert (a fact was updated)
        if (!insertQueue.remove(object)) {
            retractQueue.add(object);
        }
    }

    @Override
    public void propagateRetracts() {
        if (!retractQueue.isEmpty() || !insertQueue.isEmpty()) {
            var precomputeBuildHelper = precomputeBuildHelperSupplier.get();
            var internalNodeNetwork = precomputeBuildHelper.getNodeNetwork();
            var objectClassToRootNodes = new HashMap<Class<?>, List<AbstractRootNode<?>>>();
            var recordingTupleLifecycle = precomputeBuildHelper.getRecordingTupleLifecycle();

            invalidateCache();
            seenEntitySet.removeAll(retractQueue);
            seenFactSet.removeAll(retractQueue);

            for (var entity : seenEntitySet) {
                for (var rootNode : getRootNodes(entity, internalNodeNetwork, objectClassToRootNodes)) {
                    rootNode.insert(entity);
                }
            }

            for (var fact : seenFactSet) {
                for (var rootNode : getRootNodes(fact, internalNodeNetwork, objectClassToRootNodes)) {
                    rootNode.insert(fact);
                }
            }

            // Do not remove queued retracts from inserts; if a fact property
            // change, there will be both a retract and insert for that fact
            for (var object : insertQueue) {
                if (objectClassToIsEntitySourceClassMap.computeIfAbsent(object.getClass(),
                        precomputeBuildHelper::isSourceEntityClass)) {
                    seenEntitySet.add(object);
                } else {
                    seenFactSet.add(object);
                }
                for (var rootNode : getRootNodes(object, internalNodeNetwork, objectClassToRootNodes)) {
                    rootNode.insert(object);
                }
            }

            retractQueue.clear();
            insertQueue.clear();

            // settle the inner node network, so the inserts/retracts do not interfere
            // with the recording of the first object's tuples
            internalNodeNetwork.settle();
            recalculateTuples(internalNodeNetwork, objectClassToRootNodes, recordingTupleLifecycle);

            propagationQueue.propagateRetracts();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <A> List<AbstractRootNode<A>> getRootNodes(Object object, AbstractBavetNodeNetwork internalNodeNetwork,
            Map<Class<?>, List<AbstractRootNode<?>>> objectClassToRootNodes) {
        return (List) objectClassToRootNodes.computeIfAbsent(object.getClass(), clazz -> {
            var out = new ArrayList<AbstractRootNode<?>>();
            internalNodeNetwork.getRootNodesAcceptingType(object.getClass()).forEach(out::add);
            return out;
        });
    }

    @Override
    public void propagateUpdates() {
        propagationQueue.propagateUpdates();
        alreadyUpdatingSet.clear();
    }

    @Override
    public void propagateInserts() {
        // propagateRetracts clears/process the insertQueue
        propagationQueue.propagateInserts();
    }

    private void insertIfAbsent(Tuple_ tuple) {
        var state = tuple.getState();
        if (state != TupleState.CREATING) {
            propagationQueue.insert(tuple);
        }
    }

    private void retractIfPresent(Tuple_ tuple) {
        var state = tuple.getState();
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                // We already retracted this tuple from another list, so we
                // don't need to do anything
                return;
            }
            propagationQueue.retract(tuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
        } else {
            propagationQueue.retract(tuple, TupleState.DYING);
        }
    }

    private void invalidateCache() {
        objectToOutputTuplesMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(this::retractIfPresent);
        objectToOutputTuplesMap.clear();
        factOutputTupleList.forEach(this::retractIfPresent);
        factOutputTupleList.clear();
    }

    private void recalculateTuples(AbstractBavetNodeNetwork internalNodeNetwork,
            Map<Class<?>, List<AbstractRootNode<?>>> classToRootNodeList,
            RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle) {
        var internalTupleToOutputTupleMap =
                new IdentityHashMap<Tuple_, Tuple_>(seenEntitySet.size() + seenFactSet.size());
        for (var invalidated : seenEntitySet) {
            var mappedTuples = new ArrayList<Tuple_>();
            try (var unusedActiveRecordingLifecycle = recordingTupleLifecycle.recordInto(
                    new TupleRecorder<>(mappedTuples, internalTupleToOutputTupleMapper, internalTupleToOutputTupleMap))) {
                // Do a fake update on the object and settle the network; this will update precisely the
                // tuples mapped to this node, which will then be recorded
                classToRootNodeList.get(invalidated.getClass())
                        .forEach(node -> ((AbstractRootNode<Object>) node).update(invalidated));
                internalNodeNetwork.settle();
            }
            if (mappedTuples.isEmpty()) {
                objectToOutputTuplesMap.remove(invalidated);
            } else {
                objectToOutputTuplesMap.put(invalidated, mappedTuples);
            }
        }
        objectToOutputTuplesMap.values()
                .stream()
                .flatMap(List::stream)
                .forEach(this::insertIfAbsent);
        // Output tuples derived purely from facts are not re-emitted by any entity update above,
        // so they would never be delivered.
        // Facts never update during solving,
        // so a single recording pass over all facts is enough to capture every fact-dependent output tuple exactly once.
        // Tuples also derived from an entity were already delivered above;
        // insertIfAbsent skips them via tuple-state deduplication.
        if (!seenFactSet.isEmpty()) {
            try (var unusedActiveRecordingLifecycle =
                    recordingTupleLifecycle.recordInto(new TupleRecorder<>(factOutputTupleList,
                            internalTupleToOutputTupleMapper, internalTupleToOutputTupleMap))) {
                for (var fact : seenFactSet) {
                    classToRootNodeList.get(fact.getClass())
                            .forEach(node -> ((AbstractRootNode<Object>) node).update(fact));
                }
                internalNodeNetwork.settle();
            }
            factOutputTupleList.forEach(this::insertIfAbsent);
        }
    }

}