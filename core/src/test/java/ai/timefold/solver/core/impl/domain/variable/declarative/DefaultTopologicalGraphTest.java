package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

public class DefaultTopologicalGraphTest extends AbstractTopologicalGraphTest<DefaultTopologicalOrderGraph> {

    @Override
    protected DefaultTopologicalOrderGraph createTopologicalGraph(int graphSize) {
        return new DefaultTopologicalOrderGraph(graphSize);
    }

    @Override
    protected void verifyConsistent(DefaultTopologicalOrderGraph graph) {
        // DefaultTopologicalOrderGraph is not incremental
        // and has no datastructures to keep consistent
    }

    /**
     * Get the component members as a list.
     *
     * @param graph the graph
     * @param node The node to get the component members of.
     * @return The list of nodes in that component.
     */
    @Override
    protected List<Integer> getComponentMembers(DefaultTopologicalOrderGraph graph, int graphSize, int node) {
        return graph.getComponent(node);
    }

}
