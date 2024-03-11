package ai.timefold.solver.core.impl.score.stream.bavet.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractConditionalTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.AbstractTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class NodeBuildHelper<Score_ extends Score<Score_>> {

    private final Set<? extends ConstraintStream> activeStreamSet;
    private final AbstractScoreInliner<Score_> scoreInliner;
    private final Map<AbstractNode, BavetAbstractConstraintStream<?>> nodeCreatorMap;
    private final Map<ConstraintStream, TupleLifecycle<? extends AbstractTuple>> tupleLifecycleMap;
    private final Map<ConstraintStream, Integer> storeIndexMap;

    private List<AbstractNode> reversedNodeList;

    public NodeBuildHelper(Set<? extends ConstraintStream> activeStreamSet, AbstractScoreInliner<Score_> scoreInliner) {
        this.activeStreamSet = activeStreamSet;
        this.scoreInliner = scoreInliner;
        int activeStreamSetSize = activeStreamSet.size();
        this.nodeCreatorMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.tupleLifecycleMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.storeIndexMap = new HashMap<>(Math.max(16, activeStreamSetSize / 2));
        this.reversedNodeList = new ArrayList<>(activeStreamSetSize);
    }

    public boolean isStreamActive(ConstraintStream stream) {
        return activeStreamSet.contains(stream);
    }

    public AbstractScoreInliner<Score_> getScoreInliner() {
        return scoreInliner;
    }

    public void addNode(AbstractNode node, BavetAbstractConstraintStream<?> creator) {
        addNode(node, creator, creator);
    }

    public void addNode(AbstractNode node, BavetAbstractConstraintStream<?> creator, BavetAbstractConstraintStream<?> parent) {
        reversedNodeList.add(node);
        nodeCreatorMap.put(node, creator);
        if (!(node instanceof AbstractForEachUniNode<?>)) {
            if (parent == null) {
                throw new IllegalStateException("Impossible state: The node (" + node + ") has no parent (" + parent + ").");
            }
            putInsertUpdateRetract(parent, (TupleLifecycle<? extends AbstractTuple>) node);
        }
    }

    public void addNode(AbstractNode node, BavetAbstractConstraintStream<?> creator,
            BavetAbstractConstraintStream<?> leftParent,
            BavetAbstractConstraintStream<?> rightParent) {
        reversedNodeList.add(node);
        nodeCreatorMap.put(node, creator);
        putInsertUpdateRetract(leftParent, TupleLifecycle.ofLeft((LeftTupleLifecycle<? extends AbstractTuple>) node));
        putInsertUpdateRetract(rightParent, TupleLifecycle.ofRight((RightTupleLifecycle<? extends AbstractTuple>) node));
    }

    public <Tuple_ extends AbstractTuple> void putInsertUpdateRetract(ConstraintStream stream,
            TupleLifecycle<Tuple_> tupleLifecycle) {
        tupleLifecycleMap.put(stream, tupleLifecycle);
    }

    public <Tuple_ extends AbstractTuple> void putInsertUpdateRetract(ConstraintStream stream,
            List<? extends AbstractConstraintStream<?>> childStreamList,
            Function<TupleLifecycle<Tuple_>, AbstractConditionalTupleLifecycle<Tuple_>> tupleLifecycleFunction) {
        TupleLifecycle<Tuple_> tupleLifecycle = getAggregatedTupleLifecycle(childStreamList);
        putInsertUpdateRetract(stream, tupleLifecycleFunction.apply(tupleLifecycle));
    }

    public <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> getAggregatedTupleLifecycle(
            List<? extends ConstraintStream> streamList) {
        TupleLifecycle<Tuple_>[] tupleLifecycles = streamList.stream()
                .filter(this::isStreamActive)
                .map(s -> getTupleLifecycle(s, tupleLifecycleMap))
                .toArray(TupleLifecycle[]::new);
        switch (tupleLifecycles.length) {
            case 0:
                throw new IllegalStateException("Impossible state: None of the streamList (" + streamList
                        + ") are active.");
            case 1:
                return tupleLifecycles[0];
            default:
                return TupleLifecycle.of(tupleLifecycles);
        }
    }

    private static <Tuple_ extends AbstractTuple> TupleLifecycle<Tuple_> getTupleLifecycle(ConstraintStream stream,
            Map<ConstraintStream, TupleLifecycle<? extends AbstractTuple>> tupleLifecycleMap) {
        TupleLifecycle<Tuple_> tupleLifecycle = (TupleLifecycle<Tuple_>) tupleLifecycleMap.get(stream);
        if (tupleLifecycle == null) {
            throw new IllegalStateException("Impossible state: the stream (" + stream + ") hasn't built a node yet.");
        }
        return tupleLifecycle;
    }

    public int reserveTupleStoreIndex(ConstraintStream tupleSourceStream) {
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

    public int extractTupleStoreSize(ConstraintStream tupleSourceStream) {
        Integer lastIndex = storeIndexMap.put(tupleSourceStream, Integer.MIN_VALUE);
        return (lastIndex == null) ? 0 : lastIndex + 1;
    }

    public List<AbstractNode> destroyAndGetNodeList() {
        List<AbstractNode> nodeList = this.reversedNodeList;
        Collections.reverse(nodeList);
        this.reversedNodeList = null;
        return nodeList;
    }

    public BavetAbstractConstraintStream<?> getNodeCreatingStream(AbstractNode node) {
        return nodeCreatorMap.get(node);
    }

    public AbstractNode findParentNode(BavetAbstractConstraintStream<?> childNodeCreator) {
        if (childNodeCreator == null) { // We've recursed to the bottom without finding a parent node.
            throw new IllegalStateException(
                    "Impossible state: node-creating stream (" + childNodeCreator + ") has no parent node.");
        }
        // Look the stream up among node creators and if found, the node is the parent node.
        for (Map.Entry<AbstractNode, BavetAbstractConstraintStream<?>> entry : this.nodeCreatorMap.entrySet()) {
            if (entry.getValue() == childNodeCreator) {
                return entry.getKey();
            }
        }
        // Otherwise recurse to the parent node creator;
        // this happens for bridges, filters and other streams that do not create nodes.
        return findParentNode(childNodeCreator.getParent());
    }

}
