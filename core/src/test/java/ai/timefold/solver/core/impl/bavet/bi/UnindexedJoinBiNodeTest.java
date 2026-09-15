package ai.timefold.solver.core.impl.bavet.bi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnindexedJoinBiNodeTest {

    private static final int STORE_SIZE = 20;

    @Mock
    private TupleLifecycle<BiTuple<String, String>> downstream;

    private static final class TestTracker implements InOutTupleStorePositionTracker {

        private int left = 0;
        private int right = 0;
        private int out = 0;

        @Override
        public int reserveNextLeft() {
            return left++;
        }

        @Override
        public int reserveNextRight() {
            return right++;
        }

        @Override
        public int reserveNextOut() {
            return out++;
        }

        @Override
        public int computeStoreSize() {
            return out;
        }
    }

    private static UniTuple<String> createInputTuple(String fact) {
        // Mimics an upstream node handing off a freshly inserted (and thus active) tuple.
        var tuple = UniTuple.of(fact, STORE_SIZE);
        tuple.setState(TupleState.OK);
        return tuple;
    }

    @ParameterizedTest
    @ValueSource(longs = { 1, 2 })
    void updateLeft_refreshesLeftFact(long inputLayerDelta) {
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> true, new TestTracker());
        node.setInputLayerDelta(inputLayerDelta);
        var left = createInputTuple("L1");
        var right = createInputTuple("R1");
        node.insertLeft(left);
        node.insertRight(right);
        // Filtering joins defer their cross-match to prepareForSettle(); the network calls this before
        // every layer's propagate phase (see AbstractBavetNodeNetwork#settleLayer), so a direct node test
        // must too.
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
        verify(downstream).insert(argThat(t -> t.getA().equals("L1") && t.getB().equals("R1")));

        left.setA("L2");
        node.updateLeft(left);
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
        verify(downstream).update(argThat(t -> t.getA().equals("L2") && t.getB().equals("R1")));
    }

    @ParameterizedTest
    @ValueSource(longs = { 1, 2 })
    void updateRight_refreshesRightFact(long inputLayerDelta) {
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> true, new TestTracker());
        node.setInputLayerDelta(inputLayerDelta);
        var left = createInputTuple("L1");
        var right = createInputTuple("R1");
        node.insertLeft(left);
        node.insertRight(right);
        // Filtering joins defer their cross-match to prepareForSettle(); the network calls this before
        // every layer's propagate phase (see AbstractBavetNodeNetwork#settleLayer), so a direct node test
        // must too.
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
        verify(downstream).insert(argThat(t -> t.getA().equals("L1") && t.getB().equals("R1")));

        right.setA("R2");
        node.updateRight(right);
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
        verify(downstream).update(argThat(t -> t.getA().equals("L1") && t.getB().equals("R2")));
    }

    /**
     * A node two layers apart defers, which walks every pair twice:
     * once when the left side reconciles and once when the right side does.
     * A preload fills the node from empty, where no tuple can be stale,
     * so the node reads the opposite side on the spot instead and walks each pair once.
     */
    @Test
    void preload_walksEachPairOnce() {
        assertThat(countFilteringCalls(true)).isEqualTo(2 * 3);
        assertThat(countFilteringCalls(false)).isEqualTo(2 * 2 * 3);
    }

    /**
     * The network turns the flag back off once the load completes,
     * so a node must go back to deferring rather than stay eager for the rest of the session.
     */
    @Test
    void preloadEnded_defersAgain() {
        assertThat(countFilteringCalls(true, false)).isEqualTo(2 * 2 * 3);
    }

    private int countFilteringCalls(boolean... preloadSequence) {
        var filteringCount = new AtomicInteger();
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> {
            filteringCount.incrementAndGet();
            return true;
        }, new TestTracker());
        node.setInputLayerDelta(2); // Far enough apart that the node defers, unless it is preloading.
        for (var i = 0; i < preloadSequence.length; i++) {
            var preload = preloadSequence[i];
            if (i == 0 && !preload) {
                continue; // Do not disable preload which is already disabled.
            }
            if (preload) {
                node.preloadStarted();
            } else {
                node.preloadEnded();
            }
        }
        for (var i = 0; i < 2; i++) {
            node.insertLeft(createInputTuple("L" + i));
        }
        for (var i = 0; i < 3; i++) {
            node.insertRight(createInputTuple("R" + i));
        }
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
        return filteringCount.get();
    }

}
