package ai.timefold.solver.core.impl.bavet.bi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.junit.jupiter.api.Test;

/**
 * See {@link ActivitySupport}.
 */
class JoinBiNodeActivityTest {

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<BiTuple<String, String>> mockDownstream(boolean active) {
        TupleLifecycle<BiTuple<String, String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    private static UnindexedJoinBiNode<String, String> node(TupleLifecycle<BiTuple<String, String>> downstream) {
        var tracker = mock(InOutTupleStorePositionTracker.class);
        return new UnindexedJoinBiNode<>(downstream, null, tracker);
    }

    @Test
    void activeWhenBothSidesProduce() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isTrue();
        verify(downstream, times(1)).afterAllFactsInserted(true);
    }

    @Test
    void inactiveWhenRightCannotProduce() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(false);
        assertThat(node.isActive()).isFalse();
        verify(downstream, times(1)).afterAllFactsInserted(false);
    }

    @Test
    void inactiveWhenLeftCannotProduce() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInsertedLeft(false);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isFalse();
        verify(downstream, times(1)).afterAllFactsInserted(false);
    }

    @Test
    void inactiveWhenDownstreamInactive() {
        var downstream = mockDownstream(false);
        var node = node(downstream);
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isFalse();
    }
}
