package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractIfExistsNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractJoinNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetJoinConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.bavet.common.PropagationQueue;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.bavet.visual.NodeGraph;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public final class BavetConstraintSessionFactory<Solution_, Score_ extends Score<Score_>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BavetConstraintSessionFactory.class);
    private static final Level CONSTRAINT_WEIGHT_LOGGING_LEVEL = Level.DEBUG;

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final ConstraintMetaModel constraintMetaModel;

    public BavetConstraintSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintMetaModel constraintMetaModel) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.constraintMetaModel = Objects.requireNonNull(constraintMetaModel);
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @SuppressWarnings("unchecked")
    public BavetConstraintSession<Score_> buildSession(Solution_ workingSolution, ConstraintMatchPolicy constraintMatchPolicy,
            boolean scoreDirectorDerived, Consumer<String> nodeNetworkVisualizationConsumer) {
        var constraintWeightSupplier = solutionDescriptor.getConstraintWeightSupplier();
        var constraints = constraintMetaModel.getConstraints();
        if (constraintWeightSupplier != null) { // Fail fast on unknown constraints.
            var knownConstraints = constraints.stream()
                    .map(Constraint::getConstraintRef)
                    .collect(Collectors.toSet());
            constraintWeightSupplier.validate(workingSolution, knownConstraints);
        }
        var scoreDefinition = solutionDescriptor.<Score_> getScoreDefinition();
        var zeroScore = scoreDefinition.getZeroScore();
        var constraintStreamSet = new LinkedHashSet<BavetAbstractConstraintStream<Solution_>>();
        var constraintWeightMap = CollectionUtils.<Constraint, Score_> newHashMap(constraints.size());

        // Only log constraint weights if logging is enabled; otherwise we don't need to build the string.
        var constraintWeightLoggingEnabled = !scoreDirectorDerived && LOGGER.isEnabledForLevel(CONSTRAINT_WEIGHT_LOGGING_LEVEL);
        var constraintWeightString = constraintWeightLoggingEnabled
                ? new StringBuilder("Constraint weights for solution (%s):%n"
                        .formatted(workingSolution))
                : null;

        for (var constraint : constraints) {
            var constraintRef = constraint.getConstraintRef();
            var castConstraint = (BavetConstraint<Solution_>) constraint;
            var defaultConstraintWeight = castConstraint.getConstraintWeight();
            var constraintWeight = (Score_) castConstraint.extractConstraintWeight(workingSolution);
            if (!constraintWeight.equals(zeroScore)) {
                if (constraintWeightLoggingEnabled) {
                    if (defaultConstraintWeight != null && !defaultConstraintWeight.equals(constraintWeight)) {
                        constraintWeightString.append("  Constraint (%s) weight overridden to (%s) from (%s).%n"
                                .formatted(constraintRef, constraintWeight, defaultConstraintWeight));
                    } else {
                        constraintWeightString.append("  Constraint (%s) weight set to (%s).%n"
                                .formatted(constraintRef, constraintWeight));
                    }
                }
                /*
                 * Relies on BavetConstraintFactory#share(Stream_) occurring for all constraint stream instances
                 * to ensure there are no 2 equal ConstraintStream instances (with different child stream lists).
                 */
                castConstraint.collectActiveConstraintStreams(constraintStreamSet);
                constraintWeightMap.put(constraint, constraintWeight);
            } else {
                if (constraintWeightLoggingEnabled) {
                    /*
                     * Filter out nodes that only lead to constraints with zero weight.
                     * Note: Node sharing happens earlier, in BavetConstraintFactory#share(Stream_).
                     */
                    constraintWeightString.append("  Constraint (%s) disabled.%n"
                            .formatted(constraintRef));
                }
            }
        }

        var scoreInliner = AbstractScoreInliner.buildScoreInliner(scoreDefinition, constraintWeightMap, constraintMatchPolicy);
        if (constraintStreamSet.isEmpty()) {
            LOGGER.warn("No constraints enabled for solution ({}).", workingSolution);
            return new BavetConstraintSession<>(scoreInliner);
        }

        if (constraintWeightLoggingEnabled) {
            LOGGER.atLevel(CONSTRAINT_WEIGHT_LOGGING_LEVEL)
                    .log(constraintWeightString.toString().trim());
        }
        return new BavetConstraintSession<>(scoreInliner,
                buildNodeNetwork(workingSolution, constraintStreamSet, scoreInliner, nodeNetworkVisualizationConsumer));
    }

    private static <Solution_, Score_ extends Score<Score_>> NodeNetwork buildNodeNetwork(Solution_ workingSolution,
            Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet, AbstractScoreInliner<Score_> scoreInliner,
            Consumer<String> nodeNetworkVisualizationConsumer) {
        var buildHelper = new NodeBuildHelper<>(constraintStreamSet, scoreInliner);
        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<AbstractForEachUniNode<?>>>();
        var nodeList = buildNodeList(constraintStreamSet, buildHelper, node -> {
            if (!(node instanceof AbstractForEachUniNode<?> forEachUniNode)) {
                return;
            }
            var forEachClass = forEachUniNode.getForEachClass();
            var forEachUniNodeList =
                    declaredClassToNodeMap.computeIfAbsent(forEachClass, k -> new ArrayList<>(2));
            if (forEachUniNodeList.size() == 2) {
                // Each class can have at most two forEach nodes: one including null vars, the other excluding them.
                throw new IllegalStateException(
                        "Impossible state: For class (%s) there are already 2 nodes (%s), not adding another (%s)."
                                .formatted(forEachClass, forEachUniNodeList, forEachUniNode));
            }
            forEachUniNodeList.add(forEachUniNode);
        });
        if (nodeNetworkVisualizationConsumer != null) {
            var constraintSet = scoreInliner.getConstraints();
            var visualisation = NodeGraph.of(workingSolution, nodeList, constraintSet, buildHelper::getNodeCreatingStream,
                    buildHelper::findParentNode)
                    .buildGraphvizDOT();
            nodeNetworkVisualizationConsumer.accept(visualisation);
        }
        var layerMap = new TreeMap<Long, List<Propagator>>();
        for (var node : nodeList) {
            layerMap.computeIfAbsent(node.getLayerIndex(), k -> new ArrayList<>())
                    .add(node.getPropagator());
        }
        var layerCount = layerMap.size();
        var layeredNodes = new Propagator[layerCount][];
        for (var i = 0; i < layerCount; i++) {
            var layer = layerMap.get((long) i);
            layeredNodes[i] = layer.toArray(new Propagator[0]);
        }
        return new NodeNetwork(declaredClassToNodeMap, layeredNodes);
    }

    private static <Solution_, Score_ extends Score<Score_>> List<AbstractNode> buildNodeList(
            Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet, NodeBuildHelper<Score_> buildHelper,
            Consumer<AbstractNode> nodeProcessor) {
        /*
         * Build constraintStreamSet in reverse order to create downstream nodes first
         * so every node only has final variables (some of which have downstream node method references).
         */
        var reversedConstraintStreamList = new ArrayList<>(constraintStreamSet);
        Collections.reverse(reversedConstraintStreamList);
        for (var constraintStream : reversedConstraintStreamList) {
            constraintStream.buildNode(buildHelper);
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
    private static <Score_ extends Score<Score_>> long determineLayerIndex(AbstractNode node,
            NodeBuildHelper<Score_> buildHelper) {
        if (node instanceof AbstractForEachUniNode<?>) { // ForEach nodes, and only they, are in layer 0.
            return 0;
        } else if (node instanceof AbstractJoinNode<?, ?, ?> joinNode) {
            return determineLayerIndexOfBinaryOperation(
                    (BavetJoinConstraintStream<?>) buildHelper.getNodeCreatingStream(joinNode), buildHelper);
        } else if (node instanceof AbstractConcatNode<?, ?, ?> concatNode) {
            return determineLayerIndexOfBinaryOperation(
                    (BavetConcatConstraintStream<?>) buildHelper.getNodeCreatingStream(concatNode), buildHelper);
        } else if (node instanceof AbstractIfExistsNode<?, ?> ifExistsNode) {
            return determineLayerIndexOfBinaryOperation(
                    (BavetIfExistsConstraintStream<?>) buildHelper.getNodeCreatingStream(ifExistsNode), buildHelper);
        } else {
            var nodeCreator = (BavetAbstractConstraintStream<?>) buildHelper.getNodeCreatingStream(node);
            var parentNode = buildHelper.findParentNode(nodeCreator.getParent());
            return parentNode.getLayerIndex() + 1;
        }
    }

    private static <Score_ extends Score<Score_>> long determineLayerIndexOfBinaryOperation(
            BavetStreamBinaryOperation<?> nodeCreator, NodeBuildHelper<Score_> buildHelper) {
        var leftParent = nodeCreator.getLeftParent();
        var rightParent = nodeCreator.getRightParent();
        var leftParentNode = buildHelper.findParentNode(leftParent);
        var rightParentNode = buildHelper.findParentNode(rightParent);
        return Math.max(leftParentNode.getLayerIndex(), rightParentNode.getLayerIndex()) + 1;
    }

}
