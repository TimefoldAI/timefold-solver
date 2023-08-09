package ai.timefold.solver.constraint.streams.bavet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractIfExistsNode;
import ai.timefold.solver.constraint.streams.bavet.common.AbstractJoinNode;
import ai.timefold.solver.constraint.streams.bavet.common.AbstractNode;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetIfExistsConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.BavetJoinConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.constraint.streams.common.inliner.AbstractScoreInliner;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

public final class BavetConstraintSessionFactory<Solution_, Score_ extends Score<Score_>> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final List<BavetConstraint<Solution_>> constraintList;

    public BavetConstraintSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            List<BavetConstraint<Solution_>> constraintList) {
        this.solutionDescriptor = solutionDescriptor;
        this.constraintList = constraintList;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    public BavetConstraintSession<Score_> buildSession(boolean constraintMatchEnabled,
            Solution_ workingSolution) {
        ScoreDefinition<Score_> scoreDefinition = solutionDescriptor.getScoreDefinition();
        Score_ zeroScore = scoreDefinition.getZeroScore();
        Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet = new LinkedHashSet<>();
        Map<Constraint, Score_> constraintWeightMap = new HashMap<>(constraintList.size());
        for (BavetConstraint<Solution_> constraint : constraintList) {
            Score_ constraintWeight = constraint.extractConstraintWeight(workingSolution);
            // Filter out nodes that only lead to constraints with zero weight.
            // Note: Node sharing happens earlier, in BavetConstraintFactory#share(Stream_).
            if (!constraintWeight.equals(zeroScore)) {
                // Relies on BavetConstraintFactory#share(Stream_) occurring for all constraint stream instances
                // to ensure there are no 2 equal ConstraintStream instances (with different child stream lists).
                constraint.collectActiveConstraintStreams(constraintStreamSet);
                constraintWeightMap.put(constraint, constraintWeight);
            }
        }
        AbstractScoreInliner<Score_> scoreInliner =
                AbstractScoreInliner.buildScoreInliner(scoreDefinition, constraintWeightMap, constraintMatchEnabled);
        NodeBuildHelper<Score_> buildHelper = new NodeBuildHelper<>(constraintStreamSet, scoreInliner);
        // Build constraintStreamSet in reverse order to create downstream nodes first
        // so every node only has final variables (some of which have downstream node method references).
        List<BavetAbstractConstraintStream<Solution_>> reversedConstraintStreamList = new ArrayList<>(constraintStreamSet);
        Collections.reverse(reversedConstraintStreamList);
        for (BavetAbstractConstraintStream<Solution_> constraintStream : reversedConstraintStreamList) {
            constraintStream.buildNode(buildHelper);
        }
        List<AbstractNode> nodeList = buildHelper.destroyAndGetNodeList();
        Map<Class<?>, List<AbstractForEachUniNode<Object>>> declaredClassToNodeMap = new LinkedHashMap<>();
        long nextNodeId = 0;
        for (AbstractNode node : nodeList) {
            node.setId(nextNodeId++);
            node.setLayerIndex(determineLayerIndex(node, buildHelper));
            if (node instanceof AbstractForEachUniNode<?> forEachUniNode) {
                Class<?> forEachClass = forEachUniNode.getForEachClass();
                List<AbstractForEachUniNode<Object>> forEachUniNodeList =
                        declaredClassToNodeMap.computeIfAbsent(forEachClass, k -> new ArrayList<>());
                if (forEachUniNodeList.size() == 2) {
                    // Each class can have at most 2 forEach nodes: one including null vars, the other excluding them.
                    throw new IllegalStateException("Impossible state: For class (" + forEachClass
                            + ") there are already 2 nodes (" + forEachUniNodeList + "), not adding another ("
                            + forEachUniNode + ").");
                }
                forEachUniNodeList.add((AbstractForEachUniNode<Object>) forEachUniNode);
            }
        }
        SortedMap<Long, List<AbstractNode>> layerMap = new TreeMap<>();
        for (AbstractNode node : nodeList) {
            layerMap.computeIfAbsent(node.getLayerIndex(), k -> new ArrayList<>()).add(node);
        }
        int layerCount = layerMap.size();
        AbstractNode[][] layeredNodes = new AbstractNode[layerCount][];
        for (int i = 0; i < layerCount; i++) {
            List<AbstractNode> layer = layerMap.get((long) i);
            layeredNodes[i] = layer.toArray(new AbstractNode[0]);
        }
        return new BavetConstraintSession<>(scoreInliner, declaredClassToNodeMap, layeredNodes);
    }

    private long determineLayerIndex(AbstractNode node, NodeBuildHelper<Score_> buildHelper) {
        if (node instanceof AbstractForEachUniNode<?>) { // ForEach nodes, and only they, are in layer 0.
            return 0;
        } else if (node instanceof AbstractJoinNode<?, ?, ?> joinNode) {
            var nodeCreator = (BavetJoinConstraintStream<?>) buildHelper.getNodeCreatingStream(joinNode);
            var leftParent = nodeCreator.getLeftParent();
            var rightParent = nodeCreator.getRightParent();
            var leftParentNode = buildHelper.findParentNode(leftParent);
            var rightParentNode = buildHelper.findParentNode(rightParent);
            return Math.max(leftParentNode.getLayerIndex(), rightParentNode.getLayerIndex()) + 1;
        } else if (node instanceof AbstractIfExistsNode<?, ?> ifExistsNode) {
            var nodeCreator = (BavetIfExistsConstraintStream<?>) buildHelper.getNodeCreatingStream(ifExistsNode);
            var leftParent = nodeCreator.getLeftParent();
            var rightParent = nodeCreator.getRightParent();
            var leftParentNode = buildHelper.findParentNode(leftParent);
            var rightParentNode = buildHelper.findParentNode(rightParent);
            return Math.max(leftParentNode.getLayerIndex(), rightParentNode.getLayerIndex()) + 1;
        } else {
            var nodeCreator = (BavetAbstractConstraintStream<?>) buildHelper.getNodeCreatingStream(node);
            var parentNode = buildHelper.findParentNode(nodeCreator.getParent());
            return parentNode.getLayerIndex() + 1;
        }
    }

}
