package ai.timefold.solver.core.impl.bavet.uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * See {@link ActivitySupport}.
 * The node must count every fact it has ever seen, regardless of filter outcome;
 * filter transitions during update() must not corrupt that count.
 * Updates can arrive before the activation check runs,
 * because shadow variable initialization happens between fact insertion and the first settle.
 */
class ForEachFilteredUniNodeActivityTest {

    private final Set<String> passing = new HashSet<>();

    private ForEachFilteredUniNode<String> node(TupleLifecycle<UniTuple<String>> downstream) {
        return new ForEachFilteredUniNode<>(String.class, passing::contains, downstream, 1);
    }

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<UniTuple<String>> mockDownstream(boolean active) {
        TupleLifecycle<UniTuple<String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    @Test
    void staysActiveWhenFactStopsPassingFilterBeforeActivationCheck() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        passing.add("A");
        node.insert("A");
        passing.remove("A");
        node.update("A"); // Retracts the tuple downstream, but the fact itself is still inserted.
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isTrue();
        verify(downstream).afterAllFactsInserted(true);
    }

    @Test
    void deactivatesWhenAllFactsRetractedAfterFilterTransition() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.insert("A"); // Does not pass the filter.
        passing.add("A");
        node.update("A"); // Now passes; the fact was already counted at insert.
        node.retract("A");
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
        verify(downstream).afterAllFactsInserted(false);
    }

    @Test
    void staysActiveWhenFactNeverPassedFilter() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.insert("A"); // Does not pass the filter, but a later update might.
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isTrue();
        verify(downstream).afterAllFactsInserted(true);
    }

    @Test
    void inactiveWhenNoFactsInserted() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
        verify(downstream).afterAllFactsInserted(false);
    }
}
