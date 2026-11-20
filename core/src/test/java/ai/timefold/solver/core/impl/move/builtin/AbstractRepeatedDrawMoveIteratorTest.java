package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.DummyMove;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class AbstractRepeatedDrawMoveIteratorTest {

    @Test
    void leftWithoutMoveIsRetiredAfterProbeAttemptCountProbes() {
        var move = new DummyMove();
        var callCountMap = new HashMap<String, Integer>();
        var iterator = new CountingIterator(leftIterator("dead", "live"), callCountMap, "live", move);

        for (var i = 0; i < 1_000; i++) {
            assertThat(iterator.next()).isSameAs(move);
        }
        assertThat(callCountMap).containsEntry("dead", RetiringBiWalk.PROBE_ATTEMPT_COUNT);
    }

    @Test
    void endsOnceEveryLeftIsRetired() {
        var callCountMap = new HashMap<String, Integer>();
        // A null left never reaches tryBuildMove and is retired too.
        var iterator = new CountingIterator(leftIterator("a", null, "c"), callCountMap, null, null);

        assertThat(iterator.hasNext()).isFalse();
        assertThat(callCountMap).containsOnly(Map.entry("a", RetiringBiWalk.PROBE_ATTEMPT_COUNT),
                Map.entry("c", RetiringBiWalk.PROBE_ATTEMPT_COUNT));
    }

    private static RetiringRandomIterator<@Nullable String> leftIterator(@Nullable String... lefts) {
        var leftList = new ElementAwareArrayList<@Nullable String>();
        Collections.addAll(leftList, lefts);
        return RetiringRandomIterator.of(leftList, new Random(0));
    }

    @NullMarked
    private static final class CountingIterator extends AbstractRepeatedDrawMoveIterator<TestdataSolution, String> {

        private final Map<String, Integer> callCountMap;
        private final @Nullable String liveLeft;
        private final @Nullable Move<TestdataSolution> move;

        CountingIterator(RetiringRandomIterator<@Nullable String> leftIterator, Map<String, Integer> callCountMap,
                @Nullable String liveLeft, @Nullable Move<TestdataSolution> move) {
            super(leftIterator);
            this.callCountMap = callCountMap;
            this.liveLeft = liveLeft;
            this.move = move;
        }

        @Override
        protected @Nullable Move<TestdataSolution> tryBuildMove(String left) {
            callCountMap.merge(left, 1, Integer::sum);
            return left.equals(liveLeft) ? move : null;
        }

    }

}
