package ai.timefold.solver.core.impl.bavet.uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * See {@link ActivitySupport}.
 */
class ForEachUniNodeActivityTest {

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<UniTuple<String>> mockDownstream(boolean active) {
        TupleLifecycle<UniTuple<String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    @Test
    void unfilteredInactiveWhenNoFacts() {
        var downstream = mockDownstream(true);
        var node = new ForEachUnfilteredUniNode<>(String.class, downstream, false, 1);
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void unfilteredActiveWhenFactsExist() {
        var downstream = mockDownstream(true);
        var node = new ForEachUnfilteredUniNode<>(String.class, downstream, false, 1);
        node.insert("a");
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void unfilteredInactiveWhenDownstreamInactive() {
        var downstream = mockDownstream(false);
        var node = new ForEachUnfilteredUniNode<>(String.class, downstream, false, 1);
        node.insert("a");
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void filteredInactiveWhenNoFacts() {
        var downstream = mockDownstream(true);
        var node = new ForEachFilteredUniNode<>(String.class, s -> true, downstream, false, 1);
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void filteredActiveWhenFactInsertedEvenIfFilteredOut() {
        var downstream = mockDownstream(true);
        var node = new ForEachFilteredUniNode<>(String.class, s -> false, downstream, false, 1);
        node.insert("a");
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void filteredInactiveWhenDownstreamInactive() {
        var downstream = mockDownstream(false);
        var node = new ForEachFilteredUniNode<>(String.class, s -> true, downstream, false, 1);
        node.insert("a");
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
    }
}
