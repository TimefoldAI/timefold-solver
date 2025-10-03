package ai.timefold.solver.core.impl.bavet.uni;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.StaticPropagationQueue;
import ai.timefold.solver.core.impl.bavet.common.TupleSourceRoot;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class StaticDataUniNode<A> extends AbstractNode implements TupleSourceRoot<Object>, TupleLifecycle<UniTuple<A>> {
    private final StaticPropagationQueue<UniTuple<A>> propagationQueue;
    private final Map<Object, List<UniTuple<A>>> tupleMap = new IdentityHashMap<>(1000);
    private final int outputStoreSize;
    private final NodeNetwork nodeNetwork;
    private final RecordingTupleNode<UniTuple<A>> recordingTupleNode;
    private final Class<?>[] sourceClasses;

    public StaticDataUniNode(NodeNetwork nodeNetwork,
            RecordingTupleNode<UniTuple<A>> recordingTupleNode,
            int outputStoreSize,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        this.nodeNetwork = nodeNetwork;
        this.outputStoreSize = outputStoreSize;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
        this.sourceClasses = sourceClasses;
        this.recordingTupleNode = recordingTupleNode;
    }

    @Override
    public boolean allowsInstancesOf(Class<?> clazz) {
        for (var sourceClass : sourceClasses) {
            if (sourceClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<?>[] getSourceClasses() {
        return sourceClasses;
    }

    @Override
    public boolean supports(LifecycleOperation lifecycleOperation) {
        return true;
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    public void insert(@Nullable Object a) {
        if (tupleMap.containsKey(a)) {
            return;
        }
        tupleMap.put(a, new ArrayList<>());
        insertIntoNodeNetwork(a);
        recalculateTuples();
    }

    public void update(@Nullable Object a) {
        for (var mappedTuple : tupleMap.get(a)) {
            updateExisting(a, mappedTuple);
        }
    }

    private void updateExisting(@Nullable Object a, UniTuple<A> tuple) {
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

    public void retract(@Nullable Object a) {
        if (!tupleMap.containsKey(a)) {
            return;
        }
        tupleMap.remove(a);
        retractFromNodeNetwork(a);
        recalculateTuples();
    }

    private void insertNew(UniTuple<A> tuple) {
        var state = tuple.state;
        if (state != TupleState.CREATING) {
            propagationQueue.insert(tuple);
        }
    }

    private void retractExisting(UniTuple<A> tuple) {
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
        nodeNetwork.getTupleSourceRootNodes(toInsert.getClass()).forEach(node -> {
            ((AbstractForEachUniNode) node).insert(toInsert);
        });
    }

    private void retractFromNodeNetwork(Object toRetract) {
        nodeNetwork.getTupleSourceRootNodes(toRetract.getClass()).forEach(node -> {
            ((AbstractForEachUniNode) node).retract(toRetract);
        });
    }

    private void recalculateTuples() {
        tupleMap.values().stream().flatMap(List::stream).forEach(this::retractExisting);

        var recorder = recordingTupleNode.getTupleRecorder();
        recorder.reset();
        for (var mappedTupleEntry : tupleMap.entrySet()) {
            mappedTupleEntry.getValue().clear();
            var invalidated = mappedTupleEntry.getKey();
            recorder.recordingInto(mappedTupleEntry.getValue(), this::remapTuple, () -> {
                // Do an update on the object and settle the network; this will update precisely the
                // tuples mapped to this node, which will then be recorded
                nodeNetwork.getTupleSourceRootNodes(invalidated.getClass()).forEach(node -> {
                    ((AbstractForEachUniNode) node).update(invalidated);
                });
                nodeNetwork.settle();
            });
        }
        tupleMap.values().stream().flatMap(List::stream).forEach(this::insertNew);
    }

    private UniTuple<A> remapTuple(UniTuple<A> tuple) {
        return new UniTuple<>(tuple.factA, outputStoreSize);
    }

    @Override
    public void insert(UniTuple<A> tuple) {
        // Do nothing; this is a source node
    }

    @Override
    public void update(UniTuple<A> tuple) {
        // Do nothing; this is a source node
    }

    @Override
    public void retract(UniTuple<A> tuple) {
        // Do nothing; this is a source node
    }

}
