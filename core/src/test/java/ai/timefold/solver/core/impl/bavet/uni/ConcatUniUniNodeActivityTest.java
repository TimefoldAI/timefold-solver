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
class ConcatUniUniNodeActivityTest {

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<UniTuple<String>> mockDownstream(boolean active) {
        TupleLifecycle<UniTuple<String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    private static ConcatUniUniNode<String> node(TupleLifecycle<UniTuple<String>> downstream) {
        return new ConcatUniUniNode<>(downstream, 0, 1, 2);
    }

    @Test
    void activeWhenBothSidesProduce() {
        var node = node(mockDownstream(true));
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void activeWhenOnlyLeftProduces() {
        var node = node(mockDownstream(true));
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(false);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void activeWhenOnlyRightProduces() {
        var node = node(mockDownstream(true));
        node.afterAllFactsInsertedLeft(false);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void inactiveWhenNeitherProduces() {
        var node = node(mockDownstream(true));
        node.afterAllFactsInsertedLeft(false);
        node.afterAllFactsInsertedRight(false);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void inactiveWhenDownstreamInactive() {
        var node = node(mockDownstream(false));
        node.afterAllFactsInsertedLeft(true);
        node.afterAllFactsInsertedRight(true);
        assertThat(node.isActive()).isFalse();
    }
}
