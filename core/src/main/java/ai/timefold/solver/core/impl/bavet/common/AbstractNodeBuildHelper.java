package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;

public abstract class AbstractNodeBuildHelper<Stream_ extends BavetStream> {

    private final Set<Stream_> activeStreamSet;
    private final Map<AbstractNode, Stream_> nodeCreatorMap;
    private final Map<Stream_, TupleLifecycle<? extends AbstractTuple>> tupleLifecycleMap;
    private final Map<Stream_, Integer> storeIndexMap;

    private List<AbstractNode> reversedNodeList;

    public AbstractNodeBuildHelper(Set<Stream_> activeStreamSet) {
        this.activeStreamSet = activeStreamSet;
        int activeStreamSetSize = activeStreamSet.size();
        this.nodeCreatorMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.tupleLifecycleMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.storeIndexMap = new HashMap<>(Math.max(16, activeStreamSetSize / 2));
        this.reversedNodeList = new ArrayList<>(activeStreamSetSize);
    }

    public boolean isStreamActive(Stream_ stream) {
        return activeStreamSet.contains(stream);
    }

    public void addNode(AbstractNode node, Stream_ creator) {
        addNode(node, creator, creator);
    }

    public void addNode(AbstractNode node, Stream_ creator, Stream_ parent) {
        reversedNodeList.add(node);
        nodeCreatorMap.put(node, creator);
        if (!(node instanceof AbstractForEachUniNode<?>)) {
            if (parent == null) {
                throw new IllegalStateException("Impossible state: The node (" + node + ") has no parent (" + parent + ").");
            }
            putInsertUpdateRetract(parent, (TupleLifecycle<? extends AbstractTuple>) node);
        }
    }

    public void addNode(AbstractNode node, Stream_ creator, Stream_ leftParent, Stream_ rightParent) {
        reversedNodeList.add(node);
        nodeCreatorMap.put(node, creator);
        putInsertUpdateRetract(leftParent, TupleLifecycle.ofLeft((LeftTupleLifecycle<? extends AbstractTuple>) node));
        putInsertUpdateRetract(rightParent, TupleLifecycle.ofRight((RightTupleLifecycle<? extends AbstractTuple>) node));
    }

    public <Tuple_ extends AbstractTuple> void putInsertUpdateRetract(Stream_ stream,
            TupleLifecycle<Tuple_> tupleLifecycle) {
        tupleLifecycleMap.put(stream, tupleLifecycle);
    }

    public <Tuple_ extends AbstractTuple> void putInsertUpdateRetract(Stream_ stream, List<? extends Stream_> childStreamList,
            Function<TupleLifecycle<Tuple_>, TupleLifecycle<Tuple_>> tupleLifecycleFunction) {
        TupleLifecycle<Tuple_> tupleLifecycle = getAggregatedTupleLifecycle(childStreamList);
        putInsertUpdateRetract(stream, tupleLifecycleFunction.apply(tupleLifecycle));
    }

    @SuppressWarnings("unchecked")
    public <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_>
            getAggregatedTupleLifecycle(List<? extends Stream_> streamList) {
        var tupleLifecycles = streamList.stream()
                .filter(this::isStreamActive)
                .map(s -> getTupleLifecycle(s, tupleLifecycleMap))
                .toArray(TupleLifecycle[]::new);
        return switch (tupleLifecycles.length) {
            case 0 ->
                throw new IllegalStateException("Impossible state: None of the streamList (" + streamList + ") are active.");
            case 1 -> tupleLifecycles[0];
            default -> TupleLifecycle.aggregate(tupleLifecycles);
        };
    }

    @SuppressWarnings("unchecked")
    private static <Stream_, Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> getTupleLifecycle(Stream_ stream,
            Map<Stream_, TupleLifecycle<? extends AbstractTuple>> tupleLifecycleMap) {
        var tupleLifecycle = (TupleLifecycle<Tuple_>) tupleLifecycleMap.get(stream);
        if (tupleLifecycle == null) {
            throw new IllegalStateException("Impossible state: the stream (" + stream + ") hasn't built a node yet.");
        }
        return tupleLifecycle;
    }

    public int reserveTupleStoreIndex(Stream_ tupleSourceStream) {
        return storeIndexMap.compute(tupleSourceStream, (k, index) -> {
            if (index == null) {
                return 0;
            } else if (index < 0) {
                throw new IllegalStateException("Impossible state: the tupleSourceStream (" + k
                        + ") is reserving a store after it has been extracted.");
            } else {
                return index + 1;
            }
        });
    }

    public int extractTupleStoreSize(Stream_ tupleSourceStream) {
        Integer lastIndex = storeIndexMap.put(tupleSourceStream, Integer.MIN_VALUE);
        return (lastIndex == null) ? 0 : lastIndex + 1;
    }

    public List<AbstractNode> destroyAndGetNodeList() {
        List<AbstractNode> nodeList = this.reversedNodeList;
        Collections.reverse(nodeList);
        this.reversedNodeList = null;
        return nodeList;
    }

    public Stream_ getNodeCreatingStream(AbstractNode node) {
        return nodeCreatorMap.get(node);
    }

    public AbstractNode findParentNode(Stream_ childNodeCreator) {
        if (childNodeCreator == null) { // We've recursed to the bottom without finding a parent node.
            throw new IllegalStateException(
                    "Impossible state: node-creating stream (" + childNodeCreator + ") has no parent node.");
        }
        // Look the stream up among node creators and if found, the node is the parent node.
        for (Map.Entry<AbstractNode, Stream_> entry : this.nodeCreatorMap.entrySet()) {
            if (entry.getValue() == childNodeCreator) {
                return entry.getKey();
            }
        }
        // Otherwise recurse to the parent node creator;
        // this happens for bridges, filters and other streams that do not create nodes.
        return findParentNode(childNodeCreator.getParent());
    }

}
