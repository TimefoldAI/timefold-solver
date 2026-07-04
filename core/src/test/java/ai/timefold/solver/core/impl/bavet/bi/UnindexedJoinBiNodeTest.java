package ai.timefold.solver.core.impl.bavet.bi;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.InOutTupleStorePositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void updateLeft_refreshesLeftFact() {
        var node = new UnindexedJoinBiNode<String, String>(downstream, (a, b) -> true, new TestTracker());
        var left = createInputTuple("L1");
        var right = createInputTuple("R1");
        node.insertLeft(left);
        node.insertRight(right);
        node.getPropagator().propagateEverything();
        verify(downstream).insert(argThat(t -> t.getA().equals("L1") && t.getB().equals("R1")));

        left.setA("L2");
        node.updateLeft(left);
        node.getPropagator().propagateEverything();
        verify(downstream).update(argThat(t -> t.getA().equals("L2") && t.getB().equals("R1")));
    }

    @Test
    void updateRight_refreshesRightFact() {
        var node = new UnindexedJoinBiNode<String, String>(downstream, (a, b) -> true, new TestTracker());
        var left = createInputTuple("L1");
        var right = createInputTuple("R1");
        node.insertLeft(left);
        node.insertRight(right);
        node.getPropagator().propagateEverything();
        verify(downstream).insert(argThat(t -> t.getA().equals("L1") && t.getB().equals("R1")));

        right.setA("R2");
        node.updateRight(right);
        node.getPropagator().propagateEverything();
        verify(downstream).update(argThat(t -> t.getA().equals("L1") && t.getB().equals("R2")));
    }

}
