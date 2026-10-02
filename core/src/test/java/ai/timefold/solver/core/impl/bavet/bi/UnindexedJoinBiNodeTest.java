package ai.timefold.solver.core.impl.bavet.bi;

import static ai.timefold.solver.core.impl.bavet.common.DeferredSettleAware.MIN_DEFER_DISTANCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;
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
    @ValueSource(longs = { MIN_DEFER_DISTANCE - 1, MIN_DEFER_DISTANCE })
    void updateLeft_refreshesLeftFact(long settleDistance) {
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> true, new TestTracker());
        node.setSettleDistance(settleDistance);
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
    @ValueSource(longs = { MIN_DEFER_DISTANCE - 1, MIN_DEFER_DISTANCE })
    void updateRight_refreshesRightFact(long settleDistance) {
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> true, new TestTracker());
        node.setSettleDistance(settleDistance);
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
     * A node whose inputs are far enough apart defers, which walks every pair twice:
     * once when the left side reconciles and once when the right side does.
     * A preload fills the node from empty, where no tuple can be stale,
     * so the node reads the opposite side on the spot instead and walks each pair once.
     * <p>
     * The network turns the flag back off once the load completes
     * (see {@link AbstractBavetNodeNetwork#settle()}),
     * so the node must go back to deferring rather than stay eager for the rest of the session.
     * A control node that never preloaded, driven through the identical rounds,
     * is what "back to deferring" is measured against;
     * asserting a hard-coded walk count here would pin the drain order rather than the property.
     */
    @Test
    void preload_readsEagerlyThenDefersAgain() {
        var preloadedCount = new AtomicInteger();
        var preloadedNode = createCountingNode(preloadedCount);
        preloadedNode.preloadStarted();
        loadAndSettle(preloadedNode, 0, 2, 3);
        assertThat(preloadedCount.get())
                .as("A preload reads the opposite side on the spot, so it walks each pair exactly once.")
                .isEqualTo(2 * 3);
        preloadedNode.preloadEnded();

        var controlCount = new AtomicInteger();
        var controlNode = createCountingNode(controlCount);
        loadAndSettle(controlNode, 0, 2, 3);

        // A second round with both sides dirty; were only one side dirty,
        // the deferred drain and the eager read would walk the same pairs and the two modes would tie.
        preloadedCount.set(0);
        controlCount.set(0);
        loadAndSettle(preloadedNode, 2, 1, 1);
        loadAndSettle(controlNode, 2, 1, 1);
        assertThat(preloadedCount.get())
                .as("Once the preload ends, the node must defer exactly like one that never preloaded.")
                .isEqualTo(controlCount.get());
    }

    /**
     * The deferring baseline the preload is measured against:
     * with no preload at all, every pair is walked twice.
     */
    @Test
    void withoutPreload_walksEachPairTwice() {
        var filteringCount = new AtomicInteger();
        var node = createCountingNode(filteringCount);
        loadAndSettle(node, 0, 2, 3);
        assertThat(filteringCount.get()).isEqualTo(2 * 2 * 3);
    }

    private UnindexedJoinBiNode<String, String> createCountingNode(AtomicInteger filteringCount) {
        var node = new UnindexedJoinBiNode<>(downstream, (a, b) -> {
            filteringCount.incrementAndGet();
            return true;
        }, new TestTracker());
        // Far enough apart that the node defers, unless it is preloading.
        node.setSettleDistance(MIN_DEFER_DISTANCE);
        return node;
    }

    /**
     * Inserts {@code leftCount} left and {@code rightCount} right tuples, then settles the node.
     * Facts are named from {@code factOffset} so that a later round does not reuse an earlier round's names.
     */
    private static void loadAndSettle(UnindexedJoinBiNode<String, String> node, int factOffset, int leftCount,
            int rightCount) {
        for (var i = 0; i < leftCount; i++) {
            node.insertLeft(createInputTuple("L" + (factOffset + i)));
        }
        for (var i = 0; i < rightCount; i++) {
            node.insertRight(createInputTuple("R" + (factOffset + i)));
        }
        node.prepareForSettle();
        node.getPropagator().propagateEverything();
    }

}
