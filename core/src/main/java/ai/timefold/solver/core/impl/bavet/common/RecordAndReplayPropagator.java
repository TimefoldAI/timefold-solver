package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;

/**
 * The implementation records the tuples each object affects inside
 * an internal {@link ai.timefold.solver.core.impl.bavet.NodeNetwork} and
 * replays them on update.
 * Used by {@link AbstractPrecomputeNode} to precompute constraint streams.
 *
 * @param <Tuple_>
 */
@NullMarked
public final class RecordAndReplayPropagator<Tuple_ extends AbstractTuple>
        implements Propagator {

    private final Set<Object> retractQueue;
    private final Set<Object> updateQueue;
    private final Set<Object> insertQueue;

    private final NodeNetwork internalNodeNetwork;
    private final RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle;
    private final UnaryOperator<Tuple_> internalTupleToOutputTupleMapper;
    private final Map<Tuple_, Tuple_> internalTupleToOutputTupleMap;
    private final Map<Object, List<Tuple_>> objectToOutputTuplesMap;

    private final StaticPropagationQueue<Tuple_> propagationQueue;

    public RecordAndReplayPropagator(
            NodeNetwork internalNodeNetwork,
            RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle,
            UnaryOperator<Tuple_> internalTupleToOutputTupleMapper,
            TupleLifecycle<Tuple_> nextNodesTupleLifecycle, int size) {
        this.internalNodeNetwork = internalNodeNetwork;
        this.recordingTupleLifecycle = recordingTupleLifecycle;
        this.internalTupleToOutputTupleMapper = internalTupleToOutputTupleMapper;
        this.internalTupleToOutputTupleMap = CollectionUtils.newIdentityHashMap(size);
        this.objectToOutputTuplesMap = CollectionUtils.newIdentityHashMap(size);

        // Guesstimate that updates are dominant.
        this.retractQueue = CollectionUtils.newIdentityHashSet(size / 20);
        this.updateQueue = CollectionUtils.newIdentityHashSet((size / 20) * 18);
        this.insertQueue = CollectionUtils.newIdentityHashSet(size / 20);

        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    public RecordAndReplayPropagator(
            NodeNetwork internalNodeNetwork,
            RecordingTupleLifecycle<Tuple_> recordingTupleLifecycle,
            UnaryOperator<Tuple_> internalTupleToOutputTupleMapper,
            TupleLifecycle<Tuple_> nextNodesTupleLifecycle) {
        this(internalNodeNetwork, recordingTupleLifecycle, internalTupleToOutputTupleMapper, nextNodesTupleLifecycle, 1000);
    }

    public void insert(Object object) {
        // do not remove a retract of the same fact (a fact was updated)
        insertQueue.add(object);
    }

    public void update(Object object) {
        updateQueue.add(object);
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
            updateQueue.removeAll(retractQueue);
            updateQueue.removeAll(insertQueue);

            var allObjectSet = CollectionUtils
                    .newIdentityHashSet(insertQueue.size() + objectToOutputTuplesMap.size());

            // Do not remove queued retracts from inserts; if a fact property
            // change, there will be both a retract and insert for that fact

            allObjectSet.addAll(objectToOutputTuplesMap.keySet());
            allObjectSet.removeAll(retractQueue);
            allObjectSet.addAll(insertQueue);

            invalidateCache();
            retractQueue.forEach(objectToOutputTuplesMap::remove);

            retractQueue.clear();
            insertQueue.clear();

            allObjectSet.forEach(this::insertIntoInternalNodeNetwork);

            // settle the inner node network, so the inserts/retracts do not interfere
            // with the recording of the first object's tuples
            internalNodeNetwork.settle();
            recalculateTuples(allObjectSet);

            // Free the memory now the internal node network is no longer needed
            allObjectSet.forEach(this::retractFromInternalNodeNetwork);
            internalNodeNetwork.settle();

            propagationQueue.propagateRetracts();
        }
    }

    @Override
    public void propagateUpdates() {
        Set<Tuple_> updatedTuples = CollectionUtils.newIdentityHashSet(2 * updateQueue.size());
        for (var update : updateQueue) {
            updatedTuples.addAll(objectToOutputTuplesMap.get(update));
        }
        updateQueue.clear();
        updatedTuples.forEach(propagationQueue::update);
        propagationQueue.propagateUpdates();
    }

    @Override
    public void propagateInserts() {
        // propagateRetracts clears/process the insertQueue
        propagationQueue.propagateInserts();
    }

    private void insertIfAbsent(Tuple_ tuple) {
        var state = tuple.state;
        if (state != TupleState.CREATING) {
            propagationQueue.insert(tuple);
        }
    }

    private void retractIfPresent(Tuple_ tuple) {
        var state = tuple.state;
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

    private void insertIntoInternalNodeNetwork(Object toInsert) {
        internalNodeNetwork.getRootNodesAcceptingType(toInsert.getClass())
                .forEach(node -> ((BavetRootNode<Object>) node).insert(toInsert));
    }

    private void retractFromInternalNodeNetwork(Object toRetract) {
        internalNodeNetwork.getRootNodesAcceptingType(toRetract.getClass())
                .forEach(node -> ((BavetRootNode<Object>) node).retract(toRetract));
    }

    private void invalidateCache() {
        objectToOutputTuplesMap.values().stream().flatMap(List::stream).forEach(this::retractIfPresent);
        internalTupleToOutputTupleMap.clear();
        objectToOutputTuplesMap.clear();
    }

    private void recalculateTuples(Set<Object> allObjectSet) {
        for (var invalidated : allObjectSet) {
            var mappedTuples = new ArrayList<Tuple_>();
            try (var unusedActiveRecordingLifecycle = recordingTupleLifecycle.recordInto(
                    new TupleRecorder<>(mappedTuples, internalTupleToOutputTupleMapper,
                            (IdentityHashMap<Tuple_, Tuple_>) internalTupleToOutputTupleMap))) {
                // Do a fake update on the object and settle the network; this will update precisely the
                // tuples mapped to this node, which will then be recorded
                internalNodeNetwork.getRootNodesAcceptingType(invalidated.getClass())
                        .forEach(node -> ((BavetRootNode<Object>) node).update(invalidated));
                internalNodeNetwork.settle();
            }
            if (mappedTuples.isEmpty()) {
                objectToOutputTuplesMap.put(invalidated, Collections.emptyList());
            } else {
                objectToOutputTuplesMap.put(invalidated, mappedTuples);
            }
        }
        objectToOutputTuplesMap.values().stream().flatMap(List::stream).forEach(this::insertIfAbsent);
    }

}
