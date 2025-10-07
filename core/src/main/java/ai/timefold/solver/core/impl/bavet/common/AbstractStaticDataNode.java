package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractStaticDataNode<Tuple_ extends AbstractTuple> extends AbstractNode
        implements TupleSourceRoot<Object> {
    private final StaticPropagationQueue<Tuple_> propagationQueue;
    private final Map<Object, List<Tuple_>> tupleMap = new IdentityHashMap<>(1000);
    private final NodeNetwork nodeNetwork;
    private final RecordingTupleLifecycle<Tuple_> recordingTupleNode;
    private final Class<?>[] sourceClasses;

    public AbstractStaticDataNode(NodeNetwork nodeNetwork,
            RecordingTupleLifecycle<Tuple_> recordingTupleNode,
            TupleLifecycle<Tuple_> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        this.nodeNetwork = nodeNetwork;
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
    public final boolean supports(TupleSourceRoot.LifecycleOperation lifecycleOperation) {
        return true;
    }

    @Override
    public final void insert(Object a) {
        if (tupleMap.containsKey(a)) {
            return;
        }
        invalidateCache();
        tupleMap.put(a, new ArrayList<>());
        insertIntoNodeNetwork(a);
        recalculateTuples();
    }

    @Override
    public final void update(Object a) {
        for (var mappedTuple : tupleMap.get(a)) {
            updateExisting(a, mappedTuple);
        }
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
        if (!tupleMap.containsKey(a)) {
            return;
        }
        invalidateCache();
        tupleMap.remove(a);
        retractFromNodeNetwork(a);
        recalculateTuples();
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

    private void insertIntoNodeNetwork(Object toInsert) {
        nodeNetwork.getTupleSourceRootNodes(toInsert.getClass())
                .forEach(node -> ((AbstractForEachUniNode) node).insert(toInsert));
    }

    private void retractFromNodeNetwork(Object toRetract) {
        nodeNetwork.getTupleSourceRootNodes(toRetract.getClass())
                .forEach(node -> ((AbstractForEachUniNode) node).retract(toRetract));
    }

    private void invalidateCache() {
        tupleMap.values().stream().flatMap(List::stream).forEach(this::retractExisting);
        recordingTupleNode.getTupleRecorder().reset();
    }

    private void recalculateTuples() {
        // Settle all the inserts/retracts that happen
        nodeNetwork.settle();
        var recorder = recordingTupleNode.getTupleRecorder();
        for (var mappedTupleEntry : tupleMap.entrySet()) {
            mappedTupleEntry.getValue().clear();
            var invalidated = mappedTupleEntry.getKey();
            recorder.recordingInto(mappedTupleEntry.getValue(), this::remapTuple, () -> {
                // Do an update on the object and settle the network; this will update precisely the
                // tuples mapped to this node, which will then be recorded
                nodeNetwork.getTupleSourceRootNodes(invalidated.getClass())
                        .forEach(node -> ((AbstractForEachUniNode) node).update(invalidated));
                nodeNetwork.settle();
            });
        }
        tupleMap.values().stream().flatMap(List::stream).forEach(this::insertNew);
    }

    protected abstract Tuple_ remapTuple(Tuple_ tuple);
}
