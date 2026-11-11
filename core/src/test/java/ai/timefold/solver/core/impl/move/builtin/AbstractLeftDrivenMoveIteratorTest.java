package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.RetiringBiWalk;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.DummyMove;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class AbstractLeftDrivenMoveIteratorTest {

    @Test
    void bailOutBudgetDoesNotOverflow() {
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(0)).isZero();
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(3)).isEqualTo(30);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10)).isEqualTo(Long.MAX_VALUE / 10 * 10);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE / 10 + 1)).isEqualTo(Long.MAX_VALUE);
        assertThat(AbstractLeftDrivenMoveIterator.bailOutBudget(Long.MAX_VALUE)).isEqualTo(Long.MAX_VALUE);
    }

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
        for (var left : lefts) {
            leftList.add(left);
        }
        return RetiringRandomIterator.of(leftList, new Random(0));
    }

    private static final class CountingIterator extends AbstractLeftDrivenMoveIterator<TestdataSolution, String> {

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
