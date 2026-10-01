package ai.timefold.solver.core.impl.neighborhood.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

class RetiringBiWalkTest {

    @Test
    void aLeftIsRetriedUpToProbeAttemptCountBeforeGivingUp() {
        var leftList = new ElementAwareArrayList<String>();
        leftList.add("left");
        var leftIterator = RetiringRandomIterator.of(leftList, new Random(0));

        var attemptCount = new int[] { 0 };
        var onExhaustedCalled = new boolean[] { false };
        var walk = new NonAcceptingRetiringBiWalk(attemptCount, onExhaustedCalled);

        var found = RetiringBiWalk.advance(leftIterator, walk);

        assertThat(found).isTrue();
        assertThat(onExhaustedCalled[0]).isFalse();
        assertThat(attemptCount[0]).isEqualTo(RetiringBiWalk.PROBE_ATTEMPT_COUNT);
    }

    @Test
    void aLeftWithNoMatchIsRetiredAfterExactlyProbeAttemptCountProbes() {
        var leftCount = 4;
        var leftList = new ElementAwareArrayList<String>();
        for (var i = 0; i < leftCount; i++) {
            leftList.add("left" + i);
        }
        var leftIterator = RetiringRandomIterator.of(leftList, new Random(0));

        var createRightIteratorCallCount = new int[] { 0 };
        var walk = new NoRightValueRetiringBiWalk(createRightIteratorCallCount);

        var found = RetiringBiWalk.advance(leftIterator, walk);

        assertThat(found).isFalse();
        // Every left is drawn exactly once (retirement removes it from the pool) and probed
        // RetiringBiWalk.PROBE_ATTEMPT_COUNT times before being retired.
        assertThat(createRightIteratorCallCount[0]).isEqualTo(RetiringBiWalk.PROBE_ATTEMPT_COUNT * leftCount);
    }

    @NullMarked
    private record NonAcceptingRetiringBiWalk(int[] attemptCount, boolean[] onExhaustedCalled)
            implements
                RetiringBiWalk<String, String> {

        @Override
        public Iterator<String> createRightIterator(String left) {
            attemptCount[0]++;
            // Bails out on every attempt but the last.
            return attemptCount[0] < PROBE_ATTEMPT_COUNT ? Collections.emptyIterator() : List.of("right").iterator();
        }

        @Override
        public void accept(String left, String right) {
            // Nothing to record; this test only checks whether a pair was found.
        }

        @Override
        public void onExhausted(String left) {
            onExhaustedCalled[0] = true;
        }
    }

    @NullMarked
    private record NoRightValueRetiringBiWalk(int[] createRightIteratorCallCount)
            implements
                RetiringBiWalk<String, String> {

        @Override
        public Iterator<String> createRightIterator(String left) {
            createRightIteratorCallCount[0]++;
            return Collections.emptyIterator();
        }

        @Override
        public void accept(String left, String right) {
            throw new AssertionError("Never called: no right value ever matches.");
        }
    }
}
