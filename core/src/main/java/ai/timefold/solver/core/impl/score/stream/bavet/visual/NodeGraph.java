package ai.timefold.solver.core.impl.score.stream.bavet.visual;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetForEachUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public record NodeGraph<Solution_>(Solution_ solution, List<AbstractNode> sources, List<GraphEdge> edges,
        List<GraphSink<Solution_>> sinks) {

    public static <Solution_> NodeGraph<Solution_> of(Solution_ solution, NodeBuildHelper<?> buildHelper,
            List<AbstractNode> nodeList,
            AbstractScoreInliner<?> scoreInliner) {
        var sourceList = new ArrayList<AbstractNode>();
        var edgeList = new ArrayList<GraphEdge>();
        for (var node : nodeList) {
            var nodeCreator = buildHelper.getNodeCreatingStream(node);
            if (nodeCreator instanceof BavetForEachUniConstraintStream<?, ?>) {
                sourceList.add(node);
            } else if (nodeCreator instanceof BavetStreamBinaryOperation<?> binaryOperation) {
                var leftParent = buildHelper.findParentNode(binaryOperation.getLeftParent());
                edgeList.add(new GraphEdge(leftParent, node));
                var rightParent = buildHelper.findParentNode(binaryOperation.getRightParent());
                edgeList.add(new GraphEdge(rightParent, node));
            } else {
                var parent = buildHelper.findParentNode(nodeCreator.getParent());
                edgeList.add(new GraphEdge(parent, node));
            }
        }
        var sinkList = new ArrayList<GraphSink<Solution_>>();
        for (var constraint : scoreInliner.getConstraints()) {
            var castConstraint = (BavetConstraint<Solution_>) constraint;
            var stream = (BavetAbstractConstraintStream<?>) castConstraint.getScoringConstraintStream();
            var node = buildHelper.findParentNode(stream);
            sinkList.add(new GraphSink<>(node, castConstraint));
        }
        return new NodeGraph<>(solution, sourceList.stream().distinct().toList(),
                edgeList.stream().distinct().toList(),
                sinkList.stream().distinct().toList());
    }

    public void buildDOT(Writer writer) throws IOException {
        writer.append("digraph {\n");
        writer.append("rankdir=LR;\n");
        var sourceStream = sources.stream();
        var edgeStream = edges.stream().flatMap(edge -> Stream.of(edge.from(), edge.to()));
        // Gather all known nodes and order them by their ID.
        var allNodes = Stream.concat(sourceStream, edgeStream)
                .distinct()
                .sorted(Comparator.comparingLong(AbstractNode::getId))
                .toList();
        writer.append("label=\"Bavet Node Network for '%s'\\n%d constraints, %d nodes\"\n"
                .formatted(solution.toString(), sinks.size(), allNodes.size()));
        // Specify the edges.
        for (AbstractNode node : allNodes) {
            for (var edge : edges) {
                if (edge.from().equals(node)) {
                    writer.append(nodeId(node)).append(" -> ").append(nodeId(edge.to())).append(";\n");
                }
            }
        }
        for (int i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            writer.append(nodeId(sink.node())).append(" -> ").append(constraintId(i)).append(";\n");
        }
        // Specify visual attributes of the nodes.
        for (var node : allNodes) {
            writer.append(nodeId(node)).append(" ").append(getMetadata(node)).append(";\n");
        }
        for (int i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            writer.append(constraintId(i)).append(" ").append(getMetadata(sink, solution)).append(";\n");
        }
        // Put nodes in the same layer to appear in the same rank.
        SortedMap<Long, Set<AbstractNode>> layerMap = new TreeMap<>();
        for (var node : allNodes) {
            var layer = node.getLayerIndex();
            layerMap.computeIfAbsent(layer, k -> new LinkedHashSet<>()).add(node);
        }
        for (var entry : layerMap.entrySet()) {
            writer.append("{ rank=same; ");
            for (var node : entry.getValue()) {
                writer.append(nodeId(node)).append("; ");
            }
            writer.append("}\n");
        }
        writer.append("}");
    }

    private String getMetadata(AbstractNode node) {
        var metadata = new HashMap<String, String>();
        if (sources.contains(node)) {
            metadata.put("shape", "ellipse");
            metadata.put("style", "filled");
            metadata.put("fillcolor", "lightblue");
        } else {
            metadata.put("shape", "box");
        }
        metadata.put("label", nodeLabel(node));
        return metadata.entrySet().stream()
                .map(entry -> "%s=\"%s\"".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String getMetadata(GraphSink<Solution_> sink, Solution_ solution) {
        var constraint = sink.constraint();
        var metadata = new HashMap<String, String>();
        metadata.put("shape", "ellipse");
        metadata.put("style", "filled");
        metadata.put("fillcolor", "lightgreen");
        metadata.put("label", "%s\\n(Weight: %s)"
                .formatted(constraint.getConstraintRef().constraintName(), constraint.extractConstraintWeight(solution)));
        return metadata.entrySet().stream()
                .map(entry -> "%s=\"%s\"".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String nodeId(AbstractNode node) {
        return "node" + node.getId();
    }

    private String constraintId(int id) {
        return "impact" + id;
    }

    private String nodeLabel(AbstractNode node) {
        var className = node.getClass().getSimpleName()
                .replace("Node", "");
        if (node instanceof AbstractForEachUniNode<?> forEachNode) {
            return className + "\\n(" + forEachNode.getForEachClass().getSimpleName() + ")";
        }
        return className;
    }

}
