package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractPrecomputeNode<Tuple_ extends AbstractTuple> extends AbstractNode
        implements BavetRootNode<Object> {
    private final StaticPropagationQueue<Tuple_> propagationQueue;
    private final Map<Object, List<Tuple_>> objectToOutputTuplesMap = new IdentityHashMap<>(1000);
    private final IdentityHashMap<Tuple_, Tuple_> inputTupleToOutputTupleMap = new IdentityHashMap<>(1000);
    private final NodeNetwork innerNodeNetwork;
    private final RecordingTupleLifecycle<Tuple_> recordingTupleNode;
    private final Class<?>[] sourceClasses;
    private final Set<Object> queuedInsertSet = CollectionUtils.newIdentityHashSet(32);
    private final Set<Object> queuedUpdateSet = CollectionUtils.newIdentityHashSet(32);
    private final Set<Object> queuedRetractSet = CollectionUtils.newIdentityHashSet(32);

    protected AbstractPrecomputeNode(NodeNetwork innerNodeNetwork,
            RecordingTupleLifecycle<Tuple_> recordingTupleNode,
            TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        this.innerNodeNetwork = innerNodeNetwork;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
        this.sourceClasses = sourceClasses;
        this.recordingTupleNode = recordingTupleNode;
    }

    @Override
    public final Propagator getPropagator() {
        return propagationQueue;
    }

    @Override
    public final boolean allowsInstancesOf(Class<?> clazz) {
        for (var sourceClass : sourceClasses) {
            if (sourceClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Class<?>[] getSourceClasses() {
        return sourceClasses;
    }

    @Override
    public final boolean supports(BavetRootNode.LifecycleOperation lifecycleOperation) {
        return true;
    }

    @Override
    public final void insert(Object a) {
        // do not remove a retract of the same fact (a fact was updated)
        queuedInsertSet.add(a);
    }

    @Override
    public final void update(Object a) {
        queuedUpdateSet.add(a);
    }

    private void updateExisting(@Nullable Object a, Tuple_ tuple) {
        var state = tuple.state;
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (%s) was retracted, so it cannot update."
                        .formatted(a));
            }
            // CREATING or UPDATING is ignored; it's already in the queue.
        } else {
            propagationQueue.update(tuple);
        }
    }

    @Override
    public final void retract(Object a) {
        // remove an insert then retract (a fact was inserted but retracted before settling)
        // do not remove a retract then insert (a fact was updated)
        if (!queuedInsertSet.remove(a)) {
            queuedRetractSet.add(a);
        }
    }

    @Override
    public final void settle() {
        if (!queuedRetractSet.isEmpty() || !queuedInsertSet.isEmpty()) {
            invalidateCache();
            queuedUpdateSet.removeAll(queuedRetractSet);
            queuedUpdateSet.removeAll(queuedInsertSet);
            // Do not remove queued retracts from inserts; if a fact property
            // change, there will be both a retract and insert for that fact
            queuedRetractSet.forEach(this::retractFromInnerNodeNetwork);
            queuedInsertSet.forEach(this::insertIntoInnerNodeNetwork);
            queuedRetractSet.clear();
            queuedInsertSet.clear();

            // settle the inner node network, so the inserts/retracts do not interfere
            // with the recording of the first object's tuples
            innerNodeNetwork.settle();
            recalculateTuples();
        }
        for (var updatedObject : queuedUpdateSet) {
            for (var updatedTuple : objectToOutputTuplesMap.get(updatedObject)) {
                updateExisting(updatedObject, updatedTuple);
            }
        }
        queuedUpdateSet.clear();
    }

    private void insertNew(Tuple_ tuple) {
        var state = tuple.state;
        if (state != TupleState.CREATING) {
            propagationQueue.insert(tuple);
        }
    }

    private void retractExisting(Tuple_ tuple) {
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

    private void insertIntoInnerNodeNetwork(Object toInsert) {
        objectToOutputTuplesMap.put(toInsert, new ArrayList<>());
        innerNodeNetwork.getRootNodesAcceptingType(toInsert.getClass())
                .forEach(node -> ((AbstractForEachUniNode) node).insert(toInsert));
    }

    private void retractFromInnerNodeNetwork(Object toRetract) {
        objectToOutputTuplesMap.remove(toRetract);
        innerNodeNetwork.getRootNodesAcceptingType(toRetract.getClass())
                .forEach(node -> ((AbstractForEachUniNode) node).retract(toRetract));
    }

    private void invalidateCache() {
        objectToOutputTuplesMap.values().stream().flatMap(List::stream).forEach(this::retractExisting);
        inputTupleToOutputTupleMap.clear();
    }

    private void recalculateTuples() {
        for (var mappedTupleEntry : objectToOutputTuplesMap.entrySet()) {
            mappedTupleEntry.getValue().clear();
            var invalidated = mappedTupleEntry.getKey();
            recordingTupleNode.startRecording(
                    new TupleRecorder<>(mappedTupleEntry.getValue(), this::remapTuple, inputTupleToOutputTupleMap));

            // Do a fake update on the object and settle the network; this will update precisely the
            // tuples mapped to this node, which will then be recorded
            innerNodeNetwork.getRootNodesAcceptingType(invalidated.getClass())
                    .forEach(node -> ((AbstractForEachUniNode) node).update(invalidated));
            innerNodeNetwork.settle();

            recordingTupleNode.stopRecording();
        }
        objectToOutputTuplesMap.values().stream().flatMap(List::stream).forEach(this::insertNew);
    }

    protected abstract Tuple_ remapTuple(Tuple_ tuple);
}
