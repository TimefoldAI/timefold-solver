package ai.timefold.solver.core.impl.bavet.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.tuple.ActivitySupport;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.MapUniToUniNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * See {@link ActivitySupport}.
 */
class SingleInputNodeActivityTest {

    private static MapUniToUniNode<String, String> node(TupleLifecycle<UniTuple<String>> downstream) {
        Function<String, String> identity = s -> s;
        return new MapUniToUniNode<>(0, identity, downstream, 1);
    }

    @SuppressWarnings("unchecked")
    private static TupleLifecycle<UniTuple<String>> mockDownstream(boolean active) {
        TupleLifecycle<UniTuple<String>> downstream = mock(TupleLifecycle.class);
        when(downstream.isActive()).thenReturn(active);
        return downstream;
    }

    @Test
    void activeWhenUpstreamAndDownstreamActive() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isTrue();
    }

    @Test
    void inactiveWhenUpstreamCannotProduce() {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInserted(false);
        assertThat(node.isActive()).isFalse();
    }

    @Test
    void inactiveWhenDownstreamInactive() {
        var downstream = mockDownstream(false);
        var node = node(downstream);
        node.afterAllFactsInserted(true);
        assertThat(node.isActive()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void forwardsUpstreamCapabilityDownstream(boolean upstreamCanProduce) {
        var downstream = mockDownstream(true);
        var node = node(downstream);
        node.afterAllFactsInserted(upstreamCanProduce);
        verify(downstream).afterAllFactsInserted(upstreamCanProduce);
    }
}
