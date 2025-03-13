package ai.timefold.solver.core.impl.bavet.visual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.AbstractTwoInputNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.BavetStreamBinaryOperation;
import ai.timefold.solver.core.impl.bavet.uni.AbstractForEachUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraint;
import ai.timefold.solver.core.impl.score.stream.bavet.uni.BavetForEachUniConstraintStream;

public record NodeGraph<Solution_>(Solution_ solution, List<AbstractNode> sources,
        List<GraphEdge> edges, List<GraphSink<Solution_>> sinks) {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <Solution_, Stream_ extends BavetStream> NodeGraph<Solution_> of(Solution_ solution,
            List<AbstractNode> nodeList, Set<Constraint> constraintSet, Function<AbstractNode, Stream_> nodeToStreamFunction,
            Function<Stream_, AbstractNode> streamToParentNodeFunction) {
        var sourceList = new ArrayList<AbstractNode>();
        var edgeList = new ArrayList<GraphEdge>();
        for (var node : nodeList) {
            var nodeCreator = nodeToStreamFunction.apply(node);
            if (nodeCreator instanceof BavetForEachUniConstraintStream<?, ?>) {
                sourceList.add(node);
            } else if (nodeCreator instanceof BavetStreamBinaryOperation binaryOperation) {
                var castBinaryOperation = (BavetStreamBinaryOperation<Stream_>) binaryOperation;
                var leftParent = streamToParentNodeFunction.apply(castBinaryOperation.getLeftParent());
                edgeList.add(new GraphEdge(leftParent, node));
                var rightParent = streamToParentNodeFunction.apply(castBinaryOperation.getRightParent());
                edgeList.add(new GraphEdge(rightParent, node));
            } else {
                var parent = streamToParentNodeFunction.apply(nodeCreator.getParent());
                edgeList.add(new GraphEdge(parent, node));
            }
        }
        var sinkList = new ArrayList<GraphSink<Solution_>>();
        for (var constraint : constraintSet) {
            var castConstraint = (BavetConstraint<Solution_>) constraint;
            var stream = (BavetAbstractConstraintStream<Solution_>) castConstraint.getScoringConstraintStream();
            var node = streamToParentNodeFunction.apply((Stream_) stream);
            sinkList.add(new GraphSink<>(node, castConstraint));
        }
        return new NodeGraph<>(solution, sourceList.stream().distinct().toList(),
                edgeList.stream().distinct().toList(),
                sinkList.stream().distinct().toList());
    }

    public String buildGraphvizDOT() {
        var stringBuilder = new StringBuilder();
        var sourceStream = sources.stream();
        var edgeStream = edges.stream().flatMap(edge -> Stream.of(edge.from(), edge.to()));
        // Gather all known nodes and order them by their ID.
        var allNodes = Stream.concat(sourceStream, edgeStream)
                .distinct()
                .sorted(Comparator.comparingLong(AbstractNode::getId))
                .toList();
        stringBuilder.append(
                "    label=<<B>Bavet Node Network for '%s'</B><BR />%d constraints, %d nodes>;%n"
                        .formatted(solution.toString(), sinks.size(), allNodes.size()));
        // Specify the edges.
        for (var node : allNodes) {
            for (var edge : edges) {
                if (edge.from().equals(node)) {
                    var line = "    %s -> %s;%n".formatted(nodeId(node), nodeId(edge.to()));
                    stringBuilder.append(line);
                }
            }
        }
        for (var i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            var line = "    %s -> %s;%n".formatted(nodeId(sink.node()), constraintId(i));
            stringBuilder.append(line);
        }
        // Specify visual attributes of the nodes.
        for (var node : allNodes) {
            var line = "    %s %s;%n".formatted(nodeId(node), getMetadata(node));
            stringBuilder.append(line);
        }
        for (var i = 0; i < sinks.size(); i++) {
            var sink = sinks.get(i);
            var line = "    %s %s;%n".formatted(constraintId(i), getMetadata(sink, solution));
            stringBuilder.append(line);
        }
        // Put nodes in the same layer to appear in the same rank.
        var layerMap = new TreeMap<Long, Set<AbstractNode>>();
        for (var node : allNodes) {
            var layer = node.getLayerIndex();
            layerMap.computeIfAbsent(layer, k -> new LinkedHashSet<>()).add(node);
        }
        for (var entry : layerMap.entrySet()) {
            var line = entry.getValue().stream()
                    .map(NodeGraph::nodeId)
                    .collect(Collectors.joining("; ", "    { rank=same; ", "; }" + System.lineSeparator()));
            stringBuilder.append(line);
        }
        return """
                digraph {
                    rankdir=LR;
                %s}"""
                .formatted(stringBuilder.toString());
    }

    private static String getMetadata(AbstractNode node) {
        var metadata = getBaseDOTProperties("lightgrey", false);
        if (node instanceof AbstractForEachUniNode<?>) {
            metadata.put("style", "filled");
            metadata.put("fillcolor", "#3e00ff");
            metadata.put("fontcolor", "white");
        } else if (node instanceof AbstractTwoInputNode<?, ?>) {
            // Nodes that join get a different color.
            metadata.put("style", "filled");
            metadata.put("fillcolor", "#ff7700");
            metadata.put("fontcolor", "white");
        }
        metadata.put("label", nodeLabel(node));
        return mergeMetadata(metadata);
    }

    private static String mergeMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .map(entry -> {
                    if (entry.getKey().equals("label")) { // Labels are HTML-formatted.
                        return "%s=<%s>".formatted(entry.getKey(), entry.getValue());
                    } else {
                        return "%s=\"%s\"".formatted(entry.getKey(), entry.getValue());
                    }
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static <Solution_> String getMetadata(GraphSink<Solution_> sink, Solution_ solution) {
        var constraint = sink.constraint();
        var metadata = getBaseDOTProperties("#3423a6", true);
        metadata.put("label", "<B>%s</B><BR />(Weight: %s)"
                .formatted(constraint.getConstraintRef().constraintName(), constraint.extractConstraintWeight(solution)));
        return mergeMetadata(metadata);
    }

    private static Map<String, String> getBaseDOTProperties(String fillcolor, boolean whiteText) {
        var metadata = new HashMap<String, String>();
        metadata.put("shape", "plaintext");
        metadata.put("pad", "0.2");
        metadata.put("style", "filled");
        metadata.put("fillcolor", fillcolor);
        metadata.put("fontname", "Courier New");
        metadata.put("fontcolor", whiteText ? "white" : "black");
        return metadata;
    }

    private static String nodeId(AbstractNode node) {
        return "node" + node.getId();
    }

    private static String constraintId(int id) {
        return "impact" + id;
    }

    private static String nodeLabel(AbstractNode node) {
        var className = node.getClass().getSimpleName()
                .replace("Node", "");
        if (node instanceof AbstractForEachUniNode<?> forEachNode) {
            return "<B>%s</B><BR/>(%s)".formatted(className, forEachNode.getForEachClass().getSimpleName());
        } else {
            return "<B>%s</B>".formatted(className);
        }
    }

}
