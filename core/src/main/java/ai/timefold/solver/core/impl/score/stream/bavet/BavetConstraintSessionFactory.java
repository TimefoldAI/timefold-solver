package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractIfExistsNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractJoinNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetJoinConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.PropagationQueue;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintLibrary;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BavetConstraintSessionFactory<Solution_, Score_ extends Score<Score_>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BavetConstraintSessionFactory.class);
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final ConstraintLibrary<Score_> constraintLibrary;

    @SuppressWarnings("unchecked")
    public BavetConstraintSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintLibrary<Score_> constraintLibrary) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.constraintLibrary = Objects.requireNonNull(constraintLibrary);
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @SuppressWarnings("unchecked")
    public BavetConstraintSession<Score_> buildSession(Solution_ workingSolution, boolean constraintMatchEnabled) {
        var constraintWeightSupplier = solutionDescriptor.getConstraintWeightSupplier();
        if (constraintWeightSupplier != null) { // Fail fast on unknown constraints.
            var knownConstraints = constraintLibrary.getConstraints()
                    .stream()
                    .map(Constraint::getConstraintRef)
                    .collect(Collectors.toSet());
            constraintWeightSupplier.validate(workingSolution, knownConstraints);
        }
        var scoreDefinition = solutionDescriptor.<Score_> getScoreDefinition();
        var zeroScore = scoreDefinition.getZeroScore();
        var constraintStreamSet = new LinkedHashSet<BavetAbstractConstraintStream<Solution_>>();
        var constraintWeightMap = new HashMap<Constraint, Score_>(constraintLibrary.getConstraints().size());
        LOGGER.debug("Constraint weights for solution ({}):", workingSolution);
        for (var constraint : constraintLibrary.getConstraints()) {
            var constraintRef = constraint.getConstraintRef();
            var castConstraint = (BavetConstraint<Solution_>) constraint;
            var defaultConstraintWeight = castConstraint.getDefaultConstraintWeight();
            var constraintWeight = (Score_) castConstraint.extractConstraintWeight(workingSolution);
            if (!constraintWeight.equals(zeroScore)) {
                if (defaultConstraintWeight != null && !defaultConstraintWeight.equals(constraintWeight)) {
                    LOGGER.debug("  Constraint ({}) weight overridden to ({}) from ({}).", constraintRef, constraintWeight,
                            defaultConstraintWeight);
                }
                /*
                 * Relies on BavetConstraintFactory#share(Stream_) occurring for all constraint stream instances
                 * to ensure there are no 2 equal ConstraintStream instances (with different child stream lists).
                 */
                castConstraint.collectActiveConstraintStreams(constraintStreamSet);
                constraintWeightMap.put(constraint, constraintWeight);
            } else {
                /*
                 * Filter out nodes that only lead to constraints with zero weight.
                 * Note: Node sharing happens earlier, in BavetConstraintFactory#share(Stream_).
                 */
                LOGGER.debug("  Constraint ({}) disabled.", constraintRef);
            }
        }

        var scoreInliner = AbstractScoreInliner.buildScoreInliner(scoreDefinition, constraintWeightMap, constraintMatchEnabled);
        if (constraintStreamSet.isEmpty()) { // All constraints were disabled.
            return new BavetConstraintSession<>(scoreInliner);
        }
        /*
         * Build constraintStreamSet in reverse order to create downstream nodes first
         * so every node only has final variables (some of which have downstream node method references).
         */
        var buildHelper = new NodeBuildHelper<>(constraintStreamSet, scoreInliner);
        var reversedConstraintStreamList = new ArrayList<>(constraintStreamSet);
        Collections.reverse(reversedConstraintStreamList);
        for (var constraintStream : reversedConstraintStreamList) {
            constraintStream.buildNode(buildHelper);
        }
        var nodeList = buildHelper.destroyAndGetNodeList();
        var declaredClassToNodeMap = new LinkedHashMap<Class<?>, List<AbstractForEachUniNode<Object>>>();
        var nextNodeId = 0L;
        for (var node : nodeList) {
            /*
             * Nodes are iterated first to last, starting with forEach(), the ultimate parent.
             * Parents are guaranteed to come before children.
             */
            node.setId(nextNodeId++);
            node.setLayerIndex(determineLayerIndex(node, buildHelper));
            if (node instanceof AbstractForEachUniNode<?> forEachUniNode) {
                var forEachClass = forEachUniNode.getForEachClass();
                var forEachUniNodeList =
                        declaredClassToNodeMap.computeIfAbsent(forEachClass, k -> new ArrayList<>());
                if (forEachUniNodeList.size() == 2) {
                    // Each class can have at most two forEach nodes: one including null vars, the other excluding them.
                    throw new IllegalStateException("Impossible state: For class (" + forEachClass
                            + ") there are already 2 nodes (" + forEachUniNodeList + "), not adding another ("
                            + forEachUniNode + ").");
                }
                forEachUniNodeList.add((AbstractForEachUniNode<Object>) forEachUniNode);
            }
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
        return new BavetConstraintSession<>(scoreInliner, declaredClassToNodeMap, layeredNodes);
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
    private long determineLayerIndex(AbstractNode node, NodeBuildHelper<Score_> buildHelper) {
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

    private long determineLayerIndexOfBinaryOperation(BavetStreamBinaryOperation<?> nodeCreator,
            NodeBuildHelper<Score_> buildHelper) {
        var leftParent = nodeCreator.getLeftParent();
        var rightParent = nodeCreator.getRightParent();
        var leftParentNode = buildHelper.findParentNode(leftParent);
        var rightParentNode = buildHelper.findParentNode(rightParent);
        return Math.max(leftParentNode.getLayerIndex(), rightParentNode.getLayerIndex()) + 1;
    }

}
