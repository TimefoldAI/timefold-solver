package ai.timefold.solver.core.impl.bavet.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.tuple.AggregatedTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.LeftTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.ProfilingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.RightTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.Tuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractNodeBuildHelper<Stream_ extends BavetStream> {

    private final Set<Stream_> activeStreamSet;
    private final Map<AbstractNode, Stream_> nodeCreatorMap;
    private final Map<Stream_, TupleLifecycle<? extends Tuple>> tupleLifecycleMap;
    private final Map<Stream_, List<Set<ConstraintNodeProfileId>>> streamToProfileIdSets;
    private final Map<Stream_, Integer> storeIndexMap;

    @Nullable
    private final InnerConstraintProfiler constraintProfiler;

    @Nullable
    private List<AbstractNode> reversedNodeList;
    private long nextLifecycleProfilingId = 0;

    protected AbstractNodeBuildHelper(Set<Stream_> activeStreamSet,
            @Nullable InnerConstraintProfiler constraintProfiler) {
        this.activeStreamSet = activeStreamSet;
        int activeStreamSetSize = activeStreamSet.size();
        this.nodeCreatorMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.tupleLifecycleMap = new HashMap<>(Math.max(16, activeStreamSetSize));
        this.storeIndexMap = new HashMap<>(Math.max(16, activeStreamSetSize / 2));
        this.streamToProfileIdSets = new HashMap<>(Math.max(16, activeStreamSetSize / 2));
        this.reversedNodeList = new ArrayList<>(activeStreamSetSize);
        this.constraintProfiler = constraintProfiler;
    }

    public boolean isStreamActive(Stream_ stream) {
        return activeStreamSet.contains(stream);
    }

    public void addNode(AbstractNode node, Stream_ creator) {
        addNode(node, creator, creator);
    }

    public void addNode(AbstractNode node, Stream_ creator, Stream_ parent) {
        reversedNodeList.add(node);
        node.addLocationSet(creator.getLocationSet());
        nodeCreatorMap.put(node, creator);
        if (!(node instanceof BavetRootNode<?>)) {
            if (parent == null) {
                throw new IllegalStateException("Impossible state: The node (%s) has no parent (%s)."
                        .formatted(node, parent));
            }
            putInsertUpdateRetract(parent, (TupleLifecycle<? extends Tuple>) node);
        }
    }

    public void addNode(AbstractNode node, Stream_ creator, Stream_ leftParent, Stream_ rightParent) {
        reversedNodeList.add(node);
        node.addLocationSet(creator.getLocationSet());
        nodeCreatorMap.put(node, creator);
        putInsertUpdateRetract(leftParent, TupleLifecycle.ofLeft((LeftTupleLifecycle<? extends Tuple>) node));
        putInsertUpdateRetract(rightParent, TupleLifecycle.ofRight((RightTupleLifecycle<? extends Tuple>) node));
    }

    private void updateConstraintProfileIdSet(Stream_ stream, TupleLifecycle<?> tupleLifecycle) {
        if (tupleLifecycle instanceof ProfilingTupleLifecycle<?> profilingTupleLifecycle) {
            var affectedSets = streamToProfileIdSets.getOrDefault(stream, Collections.emptyList());
            for (var affectedSet : affectedSets) {
                affectedSet.add(profilingTupleLifecycle.profileId());
            }
        } else if (tupleLifecycle instanceof AggregatedTupleLifecycle<?> aggregated) {
            for (var innerLifecycle : aggregated.lifecycles()) {
                updateConstraintProfileIdSet(stream, innerLifecycle);
            }
        }
    }

    public <Tuple_ extends Tuple> void putInsertUpdateRetract(Stream_ stream,
            TupleLifecycle<Tuple_> tupleLifecycle) {
        if (constraintProfiler != null) {
            var out = TupleLifecycle.profiling(constraintProfiler, nextLifecycleProfilingId,
                    stream, tupleLifecycle);
            tupleLifecycleMap.put(stream, out);
            updateConstraintProfileIdSet(stream, out);

            if (tupleLifecycle instanceof Scorer<Tuple_> scorer) {
                // This is a scorer, so we can navigate up its parents
                // to find all locations corresponding to this constraint
                var queue = new ArrayDeque<BavetStream>();
                var constraintSet = new LinkedHashSet<ConstraintNodeProfileId>();
                queue.add(stream);
                while (!queue.isEmpty()) {
                    var currentStream = queue.poll();
                    var streamSets =
                            streamToProfileIdSets.computeIfAbsent((Stream_) currentStream, ignored -> new ArrayList<>());
                    streamSets.add(constraintSet);
                    var lifecycle = tupleLifecycleMap.get(currentStream);
                    if (lifecycle instanceof ProfilingTupleLifecycle<?> profilingTupleLifecycle) {
                        constraintSet.add(profilingTupleLifecycle.profileId());
                    }
                    if (currentStream instanceof BavetStreamBinaryOperation<?> binaryOperation) {
                        queue.add(binaryOperation.getLeftParent());
                        queue.add(binaryOperation.getRightParent());
                    } else if (currentStream.getParent() != null) {
                        queue.add(currentStream.getParent());
                    }
                }
                constraintProfiler.registerConstraint(scorer.getConstraintRef(), constraintSet);
            }
            nextLifecycleProfilingId++;
        } else {
            tupleLifecycleMap.put(stream, tupleLifecycle);
        }
    }

    public <Tuple_ extends Tuple> void putInsertUpdateRetract(Stream_ stream, List<? extends Stream_> childStreamList,
            UnaryOperator<TupleLifecycle<Tuple_>> tupleLifecycleFunction) {
        TupleLifecycle<Tuple_> tupleLifecycle = getAggregatedTupleLifecycle(childStreamList);
        putInsertUpdateRetract(stream, tupleLifecycleFunction.apply(tupleLifecycle));
    }

    @SuppressWarnings("unchecked")
    public <Tuple_ extends Tuple> TupleLifecycle<Tuple_>
            getAggregatedTupleLifecycle(List<? extends Stream_> streamList) {
        var tupleLifecycles = streamList.stream()
                .filter(this::isStreamActive)
                .map(s -> getTupleLifecycle(s, tupleLifecycleMap))
                .toArray(TupleLifecycle[]::new);
        return switch (tupleLifecycles.length) {
            case 0 ->
                throw new IllegalStateException("Impossible state: None of the streamList (%s) are active."
                        .formatted(streamList));
            case 1 -> tupleLifecycles[0];
            default -> TupleLifecycle.aggregate(tupleLifecycles);
        };
    }

    @SuppressWarnings("unchecked")
    private static <Stream_, Tuple_ extends Tuple> TupleLifecycle<Tuple_> getTupleLifecycle(Stream_ stream,
            Map<Stream_, TupleLifecycle<? extends Tuple>> tupleLifecycleMap) {
        var tupleLifecycle = (TupleLifecycle<Tuple_>) tupleLifecycleMap.get(stream);
        if (tupleLifecycle == null) {
            throw new IllegalStateException("Impossible state: the stream (%s) hasn't built a node yet."
                    .formatted(stream));
        }
        return tupleLifecycle;
    }

    public int reserveTupleStoreIndex(Stream_ tupleSourceStream) {
        return storeIndexMap.compute(tupleSourceStream, (k, index) -> {
            if (index == null) {
                return 0;
            } else if (index < 0) {
                throw new IllegalStateException(
                        "Impossible state: the tupleSourceStream (%s) is reserving a store after it has been extracted."
                                .formatted(tupleSourceStream));
            } else {
                return index + 1;
            }
        });
    }

    public int extractTupleStoreSize(Stream_ tupleSourceStream) {
        Integer lastIndex = storeIndexMap.put(tupleSourceStream, Integer.MIN_VALUE);
        return (lastIndex == null) ? 0 : lastIndex + 1;
    }

    public InOutTupleStorePositionTracker getTupleStorePositionTracker(Stream_ tupleSourceStream, Stream_ leftParentSource,
            Stream_ rightParentSource) {
        return new TupleStoreManager<>(this, tupleSourceStream, leftParentSource, rightParentSource);
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
            throw new IllegalStateException("Impossible state: node-creating stream (%s) has no parent node."
                    .formatted(childNodeCreator));
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

    public static NodeNetwork buildNodeNetwork(List<AbstractNode> nodeList,
            Map<Class<?>, List<BavetRootNode<?>>> declaredClassToNodeMap,
            AbstractNodeBuildHelper<?> nodeBuildHelper) {
        var layerMap = new TreeMap<Long, List<Propagator>>();
        var profiler = nodeBuildHelper.constraintProfiler;
        for (var node : nodeList) {
            var layer = node.getLayerIndex();
            var propagator = node.getPropagator();
            if (profiler != null) {
                var profileKey = nodeBuildHelper.nextLifecycleProfilingId;
                nodeBuildHelper.nextLifecycleProfilingId++;
                var profileId = new ConstraintNodeProfileId(profileKey, node.getStreamKind(), node.getLocationSet(), true);
                nodeBuildHelper.constraintProfiler.register(profileId);
                propagator = new ProfilingPropagator(profiler, profileId, propagator);
                var stream = nodeBuildHelper.nodeCreatorMap.get(node);
                for (var affectedSet : nodeBuildHelper.streamToProfileIdSets.get(stream)) {
                    affectedSet.add(profileId);
                }
            }
            layerMap.computeIfAbsent(layer, k -> new ArrayList<>())
                    .add(propagator);
        }
        var layerCount = layerMap.size();
        var layeredNodes = new Propagator[layerCount][];
        for (var i = 0; i < layerCount; i++) {
            var layer = layerMap.get((long) i);
            layeredNodes[i] = layer.toArray(new Propagator[0]);
        }
        return new NodeNetwork(declaredClassToNodeMap, layeredNodes, nodeBuildHelper.constraintProfiler);
    }

    public <BuildHelper_ extends AbstractNodeBuildHelper<Stream_>> List<AbstractNode> buildNodeList(Set<Stream_> streamSet,
            BuildHelper_ buildHelper, BiConsumer<Stream_, BuildHelper_> nodeBuilder, Consumer<AbstractNode> nodeProcessor) {
        /*
         * Build streamSet in reverse order to create downstream nodes first
         * so every node only has final variables (some of which have downstream node method references).
         */
        var reversedStreamList = new ArrayList<>(streamSet);
        Collections.reverse(reversedStreamList);
        for (var constraintStream : reversedStreamList) {
            nodeBuilder.accept(constraintStream, buildHelper);
        }
        var nodeList = buildHelper.destroyAndGetNodeList();
        var nextNodeId = 0L;
        for (var node : nodeList) {
            /*
             * Nodes are iterated first to last, starting with forEach(), the ultimate parent.
             * Parents are guaranteed to come before children.
             */
            node.setId(nextNodeId++);
            node.setLayerIndex(determineLayerIndex(node, buildHelper));
            nodeProcessor.accept(node);
        }
        return nodeList;
    }

    /**
     * Nodes are propagated in layers.
     * See {@link PropagationQueue} and {@link AbstractNode} for details.
     * This method determines the layer of each node.
     * It does so by reverse-engineering the parent nodes of each node.
     * Nodes without parents (forEach nodes) are in layer 0.
     * Nodes with parents are one layer above their parents.
     * Some nodes have multiple parents, such as {@link AbstractJoinNode} and {@link AbstractIfExistsNode}.
     * These are one layer above the highest parent.
     * This is done to ensure that, when a child node starts propagating, all its parents have already propagated.
     *
     * @param node never null
     * @param buildHelper never null
     * @return at least 0
     */
    @SuppressWarnings("unchecked")
    private static <Stream_ extends BavetStream> long determineLayerIndex(AbstractNode node,
            AbstractNodeBuildHelper<Stream_> buildHelper) {
        if (node instanceof BavetRootNode<?>) { // Root nodes, and only they, are in layer 0.
            return 0;
        } else if (node instanceof AbstractTwoInputNode<?, ?> joinNode) {
            var nodeCreator = (BavetStreamBinaryOperation<?>) buildHelper.getNodeCreatingStream(joinNode);
            var leftParent = (Stream_) nodeCreator.getLeftParent();
            var rightParent = (Stream_) nodeCreator.getRightParent();
            var leftParentNode = buildHelper.findParentNode(leftParent);
            var rightParentNode = buildHelper.findParentNode(rightParent);
            return Math.max(leftParentNode.getLayerIndex(), rightParentNode.getLayerIndex()) + 1;
        } else {
            var nodeCreator = buildHelper.getNodeCreatingStream(node);
            var parentNode = buildHelper.findParentNode(nodeCreator.getParent());
            return parentNode.getLayerIndex() + 1;
        }
    }

}
