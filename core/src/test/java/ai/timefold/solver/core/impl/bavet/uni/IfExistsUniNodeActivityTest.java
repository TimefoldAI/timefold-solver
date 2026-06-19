package ai.timefold.solver.core.impl.bavet.uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.common.tuple.InTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * See {@link ActivitySupport}.
 */
class IfExistsUniNodeActivityTest {

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<UniTuple<String>> mockDownstream(boolean active) {
        TupleLifecycle<UniTuple<String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    private static UnindexedIfExistsUniNode<String, String> node(boolean shouldExist,
            TupleLifecycle<UniTuple<String>> downstream) {
        var tracker = mock(InTupleStorePositionTracker.class);
        return new UnindexedIfExistsUniNode<>(shouldExist, downstream, tracker);
    }

    private static void initBothSides(UnindexedIfExistsUniNode<String, String> node, boolean left, boolean right) {
        node.afterAllFactsInsertedLeft(left);
        node.afterAllFactsInsertedRight(right);
    }

    @Test
    void ifExistsActiveWhenBothProduce() {
        var node = node(true, mockDownstream(true));
        initBothSides(node, true, true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void ifExistsInactiveWhenRightEmpty() {
        var node = node(true, mockDownstream(true));
        initBothSides(node, true, false);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void ifExistsInactiveWhenLeftEmpty() {
        var node = node(true, mockDownstream(true));
        initBothSides(node, false, true);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void ifNotExistsActiveWhenRightEmpty() {
        var node = node(false, mockDownstream(true));
        initBothSides(node, true, false);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void ifNotExistsActiveWhenRightProduces() {
        var node = node(false, mockDownstream(true));
        initBothSides(node, true, true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void ifNotExistsInactiveWhenLeftEmpty() {
        var node = node(false, mockDownstream(true));
        initBothSides(node, false, false);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void ifNotExistsInactiveWhenDownstreamInactive() {
        var node = node(false, mockDownstream(false));
        initBothSides(node, true, false);
        assertThat(node.isActive()).isFalse();
    }
}
