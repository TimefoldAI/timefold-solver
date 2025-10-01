package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.function.TriConsumer;

import org.junit.jupiter.api.Test;

public abstract class AbstractTopologicalGraphTest<Graph_ extends TopologicalOrderGraph> {
    /**
     * Create a topological graph of the given size
     * 
     * @param graphSize The size of the graph
     * @return a new {@link Graph_}
     */
    protected abstract Graph_ createTopologicalGraph(int graphSize);

    /**
     * Verify the graph datastructures are consistent
     * 
     * @param graph The graph being verified
     */
    protected abstract void verifyConsistent(Graph_ graph);

    /**
     * Get the component members as a list.
     *
     * @param graph the graph
     * @param node The node to get the component members of.
     * @return The list of nodes in that component.
     */
    protected abstract List<Integer> getComponentMembers(Graph_ graph, int graphSize, int node);

    @SafeVarargs
    private static void assertTopologicalOrder(TopologicalOrderGraph graph, List<Integer>... expectedTopologicalNodeOrder) {
        for (var i = 0; i < expectedTopologicalNodeOrder.length; i++) {
            var members = expectedTopologicalNodeOrder[i];
            for (var j = 0; j < i; j++) {
                var lowerMembers = expectedTopologicalNodeOrder[j];
                for (var a : members) {
                    for (var b : lowerMembers) {
                        var aOrder = graph.getTopologicalOrder(a).order();
                        var bOrder = graph.getTopologicalOrder(b).order();
                        assertThat(aOrder)
                                .withFailMessage(
                                        () -> "Expected topological order of %d (%d) to be greater than %d (%d) in graph %s"
                                                .formatted(
                                                        a, aOrder, b, bOrder, graph))
                                .isGreaterThan(bOrder);
                    }
                }
            }
        }
    }

    private static <T> List<List<T>> permutations(List<T> source) {
        var out = new ArrayList<List<T>>();
        if (source.isEmpty()) {
            out.add(new ArrayList<>());
            return out;
        }
        var head = source.get(0);
        var tailPermutations = permutations(source.subList(1, source.size()));
        for (var tailPermutation : tailPermutations) {
            for (var i = 0; i <= tailPermutation.size(); i++) {
                var permutationWithHead = new ArrayList<>(tailPermutation);
                permutationWithHead.add(i, head);
                out.add(permutationWithHead);
            }
        }
        return out;
    }

    private static <T> List<List<T>> permutations(List<T> source, int limit) {
        var out = new ArrayList<List<T>>();
        if (source.isEmpty()) {
            out.add(new ArrayList<>());
            return out;
        }
        if (limit <= 1) {
            out.add(new ArrayList<>(source));
            return out;
        }
        var head = source.get(0);
        var tailPermutations = permutations(source.subList(1, source.size()), limit / source.size());
        for (var tailPermutation : tailPermutations) {
            for (var i = 0; i <= tailPermutation.size(); i++) {
                var permutationWithHead = new ArrayList<>(tailPermutation);
                permutationWithHead.add(i, head);
                out.add(permutationWithHead);
            }
        }
        return out;
    }

    void assertAllPermutations(int size, List<List<Integer>> graphEdges,
            TriConsumer<Graph_, ToIntFunction<Integer>, BitSet> asserter) {
        var nodes = new ArrayList<Integer>();
        for (var i = 0; i < size; i++) {
            nodes.add(i);
        }

        for (var renamedNodes : permutations(nodes)) {
            var changed = new BitSet();
            var renamedEdges = new ArrayList<List<Integer>>();
            for (var fromNode = 0; fromNode < size; fromNode++) {
                var renamedFromNode = renamedNodes.get(fromNode);
                for (var toNode : graphEdges.get(fromNode)) {
                    var renamedToNode = renamedNodes.get(toNode);
                    renamedEdges.add(List.of(renamedFromNode, renamedToNode));
                }
            }
            for (var edgesPermutation : permutations(renamedEdges)) {
                var graph = createTopologicalGraph(size);
                for (var edge : edgesPermutation) {
                    graph.addEdge(edge.get(0), edge.get(1));
                }
                graph.commitChanges(changed);
                asserter.accept(graph, renamedNodes::get, changed);
            }
        }
    }

    // Used for tests where there are simply too many permutations to test
    void assertSomePermutations(int size, List<List<Integer>> graphEdges,
            TriConsumer<Graph_, ToIntFunction<Integer>, BitSet> asserter) {
        var count = 0;
        var nodes = new ArrayList<Integer>();

        for (var i = 0; i < size; i++) {
            nodes.add(i);
        }

        for (var renamedNodes : permutations(nodes)) {
            var renamedEdges = new ArrayList<List<Integer>>();
            for (var fromNode = 0; fromNode < size; fromNode++) {
                var renamedFromNode = renamedNodes.get(fromNode);
                for (var toNode : graphEdges.get(fromNode)) {
                    var renamedToNode = renamedNodes.get(toNode);
                    renamedEdges.add(List.of(renamedFromNode, renamedToNode));
                }
            }
            Collections.shuffle(renamedEdges, new Random(count));
            var edgePermutations = permutations(renamedEdges, 1_000);

            for (var edgesPermutation : edgePermutations) {
                var changed = new BitSet();
                var graph = createTopologicalGraph(size);
                for (var edge : edgesPermutation) {
                    graph.addEdge(edge.get(0), edge.get(1));
                }
                graph.commitChanges(changed);
                asserter.accept(graph, renamedNodes::get, changed);
                count++;
            }
        }
    }

    @Test
    void testNoLoops() {
        final var GRAPH_SIZE = 5;
        assertAllPermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(3),
                List.of(3),
                List.of(4),
                List.of()), (graph, mapper, changed) -> {
                    verifyConsistent(graph);
                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1), mapper.applyAsInt(2)),
                            List.of(mapper.applyAsInt(3)),
                            List.of(mapper.applyAsInt(4)));

                    assertThat(changed.cardinality()).isZero();
                });
    }

    @Test
    void testNoLoopsRemoveEdge() {
        final var GRAPH_SIZE = 5;
        assertAllPermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(3),
                List.of(3),
                List.of(4),
                List.of()), (graph, mapper, changed) -> {
                    verifyConsistent(graph);

                    graph.removeEdge(mapper.applyAsInt(2), mapper.applyAsInt(3));
                    graph.commitChanges(changed);

                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1)),
                            List.of(mapper.applyAsInt(3)),
                            List.of(mapper.applyAsInt(4)));

                    // it is okay for 2 to be at the same level as 1, 3 or 4; the only requirement for
                    // it is to be after 0.
                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)), List.of(mapper.applyAsInt(2)));

                    assertThat(changed.cardinality()).isZero();
                });
    }

    @Test
    void testLoops() {
        final var GRAPH_SIZE = 5;
        assertAllPermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(3),
                List.of(3),
                List.of(4, 1),
                List.of()), (graph, mapper, changed) -> {
                    verifyConsistent(graph);
                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1), mapper.applyAsInt(2), mapper.applyAsInt(3)),
                            List.of(mapper.applyAsInt(4)));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(0)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(0));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(1))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(1),
                            mapper.applyAsInt(3));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(2)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(2));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(3))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(1),
                            mapper.applyAsInt(3));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(4)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(4));
                    assertThat(changed.cardinality()).isEqualTo(2);
                    assertThat(changed.get(mapper.applyAsInt(1))).isTrue();
                    assertThat(changed.get(mapper.applyAsInt(3))).isTrue();
                });
    }

    @Test
    void testLoopRemoveEdgeInLoop() {
        final var GRAPH_SIZE = 5;
        assertAllPermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(3),
                List.of(3),
                List.of(4, 1),
                List.of()), (graph, mapper, changed) -> {
                    changed.clear();
                    graph.removeEdge(mapper.applyAsInt(3), mapper.applyAsInt(1));
                    graph.commitChanges(changed);

                    verifyConsistent(graph);
                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1), mapper.applyAsInt(2)),
                            List.of(mapper.applyAsInt(3)),
                            List.of(mapper.applyAsInt(4)));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(0)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(0));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(1)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(1));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(2)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(2));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(3)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(3));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(4)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(4));
                    assertThat(changed.cardinality()).isEqualTo(2);
                    assertThat(changed.get(mapper.applyAsInt(1))).isTrue();
                    assertThat(changed.get(mapper.applyAsInt(3))).isTrue();
                });
    }

    @Test
    void testConnectingTwoLoops() {
        final var GRAPH_SIZE = 6;
        assertSomePermutations(6, List.of(
                List.of(1, 2),
                List.of(3, 2),
                List.of(4),
                List.of(5, 1),
                List.of(5, 2, 3),
                List.of()), (graph, mapper, changed) -> {
                    verifyConsistent(graph);
                    assertTopologicalOrder(graph, List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1), mapper.applyAsInt(2), mapper.applyAsInt(3), mapper.applyAsInt(4)),
                            List.of(mapper.applyAsInt(5)));
                    var cycle = new Integer[] { mapper.applyAsInt(1),
                            mapper.applyAsInt(2),
                            mapper.applyAsInt(3),
                            mapper.applyAsInt(4)
                    };
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(0)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(0));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(1))).containsExactlyInAnyOrder(cycle);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(2))).containsExactlyInAnyOrder(cycle);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(3))).containsExactlyInAnyOrder(cycle);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(4))).containsExactlyInAnyOrder(cycle);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(5)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(5));
                    assertThat(changed.cardinality()).isEqualTo(4);
                    for (var node : cycle) {
                        assertThat(changed.get(node)).isTrue();
                    }
                });
    }

    @Test
    void testConnectingTwoLoopsRemoveJoiningEdge() {
        final var GRAPH_SIZE = 6;
        assertSomePermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(3, 2),
                List.of(4),
                List.of(5, 1),
                List.of(5, 2, 3),
                List.of()), (graph, mapper, changed) -> {
                    changed.clear();
                    graph.removeEdge(mapper.applyAsInt(4), mapper.applyAsInt(3));
                    graph.commitChanges(changed);

                    verifyConsistent(graph);
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(0)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(1)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(0)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(2)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(0)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(3)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(0)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(4)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(0)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(5)));

                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(1)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(5)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(2)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(5)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(3)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(5)));
                    assertThat(graph.getTopologicalOrder(mapper.applyAsInt(4)))
                            .isLessThan(graph.getTopologicalOrder(mapper.applyAsInt(5)));

                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(0)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(0));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(1))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(1),
                            mapper.applyAsInt(3));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(2))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(2),
                            mapper.applyAsInt(4));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(3))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(1),
                            mapper.applyAsInt(3));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(4))).containsExactlyInAnyOrder(
                            mapper.applyAsInt(2),
                            mapper.applyAsInt(4));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(5)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(5));

                    assertThat(changed.cardinality()).isZero();
                });
    }

    @Test
    void testConnectingTwoLoopsRemoveNonJoiningEdge() {
        final var GRAPH_SIZE = 6;
        assertSomePermutations(GRAPH_SIZE, List.of(
                List.of(1, 2),
                List.of(2),
                List.of(4, 3),
                List.of(4, 5),
                List.of(5, 1),
                List.of()), (graph, mapper, changed) -> {
                    changed.clear();
                    graph.removeEdge(mapper.applyAsInt(2), mapper.applyAsInt(4));
                    graph.commitChanges(changed);

                    verifyConsistent(graph);
                    assertTopologicalOrder(graph,
                            List.of(mapper.applyAsInt(0)),
                            List.of(mapper.applyAsInt(1), mapper.applyAsInt(2), mapper.applyAsInt(3), mapper.applyAsInt(4)),
                            List.of(mapper.applyAsInt(5)));

                    var loop = new Integer[] { mapper.applyAsInt(1), mapper.applyAsInt(2), mapper.applyAsInt(3),
                            mapper.applyAsInt(4) };
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(0)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(0));
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(1))).containsExactlyInAnyOrder(loop);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(2))).containsExactlyInAnyOrder(loop);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(3))).containsExactlyInAnyOrder(loop);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(4))).containsExactlyInAnyOrder(loop);
                    assertThat(getComponentMembers(graph, GRAPH_SIZE, mapper.applyAsInt(5)))
                            .containsExactlyInAnyOrder(mapper.applyAsInt(5));

                    assertThat(changed.cardinality()).isZero();
                });
    }
}
