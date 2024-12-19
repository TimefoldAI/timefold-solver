package ai.timefold.solver.core.impl.score.stream.bavet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetForEachUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public record NodeGraph(List<AbstractNode> sources, List<GraphEdge> edges, List<GraphSink> sinks) {

    public static NodeGraph of(NodeBuildHelper<?> buildHelper, List<AbstractNode> nodeList,
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
        var sinkList = new ArrayList<GraphSink>();
        for (var constraint : scoreInliner.getConstraints()) {
            var castConstraint = (BavetConstraint<?>) constraint;
            var stream = (BavetAbstractConstraintStream<?>) castConstraint.getScoringConstraintStream();
            var node = buildHelper.findParentNode(stream);
            sinkList.add(new GraphSink(node, castConstraint));
        }
        return new NodeGraph(sourceList.stream().distinct().toList(),
                edgeList.stream().distinct().toList(),
                sinkList.stream().distinct().toList());
    }

    public boolean isSource(AbstractNode node) {
        return sources.contains(node);
    }

    public String buildDOT() {
        var sb = new StringBuilder("digraph G {\n");
        var allNodes = new LinkedHashSet<AbstractNode>();
        var queue = new LinkedHashSet<AbstractNode>();
        queue.addAll(sources);
        queue.addAll(edges.stream().map(GraphEdge::to).toList());
        queue.addAll(edges.stream().map(GraphEdge::from).toList());
        while (!queue.isEmpty()) {
            var node = queue.iterator().next();
            queue.remove(node);
            allNodes.add(node);
            for (var edge : edges) {
                if (edge.from().equals(node)) {
                    sb.append(nodeId(node)).append(" -> ").append(nodeId(edge.to())).append(";\n");
                    queue.add(edge.to());
                }
            }
        }
        for (int i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            sb.append(nodeId(sink.node())).append(" -> ").append(constraintId(i)).append(";\n");
        }
        for (var node : allNodes) {
            sb.append(nodeId(node)).append(" ").append(getMetadata(node)).append(";\n");
        }
        for (int i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            sb.append(constraintId(i)).append(" ").append(getMetadata(sink)).append(";\n");
        }
        return sb.append("}").toString();
    }

    private String getMetadata(AbstractNode node) {
        var metadata = new HashMap<String, String>();
        if (isSource(node)) {
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

    private String getMetadata(GraphSink sink) {
        var metadata = new HashMap<String, String>();
        metadata.put("shape", "ellipse");
        metadata.put("style", "filled");
        metadata.put("fillcolor", "lightgreen");
        metadata.put("label", sink.constraint().getConstraintRef().constraintName());
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
            return className + "(" + forEachNode.getForEachClass().getSimpleName() + ")";
        }
        return className;
    }

}
