package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AffectedEntitiesUpdaterTest {

    @Test
    void createNodeToEntityNodes() {
        var a = "a";
        var b = "b";
        var c = "c";

        var a1 = Mockito.mock(GraphNode.class);
        when(a1.entity()).thenReturn(a);
        when(a1.entityId()).thenReturn(0);
        when(a1.graphNodeId()).thenReturn(0);

        var a2 = Mockito.mock(GraphNode.class);
        when(a2.entity()).thenReturn(a);
        when(a2.entityId()).thenReturn(0);
        when(a2.graphNodeId()).thenReturn(1);

        var b1 = Mockito.mock(GraphNode.class);
        when(b1.entity()).thenReturn(b);
        when(b1.entityId()).thenReturn(1);
        when(b1.graphNodeId()).thenReturn(2);

        var b2 = Mockito.mock(GraphNode.class);
        when(b2.entity()).thenReturn(b);
        when(b2.entityId()).thenReturn(1);
        when(b2.graphNodeId()).thenReturn(3);

        var b3 = Mockito.mock(GraphNode.class);
        when(b3.entity()).thenReturn(b);
        when(b3.entityId()).thenReturn(1);
        when(b3.graphNodeId()).thenReturn(4);

        var c1 = Mockito.mock(GraphNode.class);
        when(c1.entity()).thenReturn(c);
        when(c1.entityId()).thenReturn(2);
        when(c1.graphNodeId()).thenReturn(5);

        var map = new HashMap<Object, List<GraphNode<Object>>>();

        map.put(a, List.of(a1, a2));
        map.put(b, List.of(b1, b2, b3));
        map.put(c, List.of(c1));

        var result = AffectedEntitiesUpdater.createNodeToEntityNodes(3,
                List.of(a1, b1, a2, c1, b3, b2),
                map::get);

        assertThat(result[0]).containsExactlyInAnyOrder(0, 1);
        assertThat(result[1]).containsExactlyInAnyOrder(2, 3, 4);
        assertThat(result[2]).containsExactlyInAnyOrder(5);
    }
}