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

class AbstractSingleDrawMoveIteratorTest {

    @Test
    void retiresLeftWithoutMoveAfterOneDraw() {
        var iterator = new SingleDrawIterator(leftIterator("dead", "live"), Map.of("dead", 0.0, "live", 1.0));

        for (var i = 0; i < 1_000; i++) {
            assertThat(iterator.next()).isNotNull();
        }
        assertThat(iterator.tryCountMap).containsEntry("dead", 1);
        assertThat(iterator.hasMoveCountMap).containsOnly(Map.entry("dead", RetiringBiWalk.PROBE_ATTEMPT_COUNT));
    }

    @Test
    void keepsLeftWithRareMoveAndAsksOnlyOnce() {
        var iterator = new SingleDrawIterator(leftIterator("rare"), Map.of("rare", 0.01));

        for (var i = 0; i < 100; i++) {
            assertThat(iterator.next()).isNotNull();
        }
        assertThat(iterator.hasMoveCountMap).containsOnly(Map.entry("rare", 1));
    }

    @Test
    void givesEachMoveTheSameChance() {
        // "often" has 4 times more moves than "seldom", so it succeeds 4 times more often per draw.
        var iterator = new SingleDrawIterator(leftIterator("often", "seldom"), Map.of("often", 0.8, "seldom", 0.2));

        var moveCountMap = new HashMap<String, Integer>();
        for (var i = 0; i < 50_000; i++) {
            moveCountMap.merge(((DummyMove) iterator.next()).getCode(), 1, Integer::sum);
        }
        assertThat(moveCountMap.get("often") / (double) moveCountMap.get("seldom")).isBetween(4 * 0.7, 4 * 1.3);
    }

    private static RetiringRandomIterator<@Nullable String> leftIterator(@Nullable String... lefts) {
        var leftList = new ElementAwareArrayList<@Nullable String>();
        Collections.addAll(leftList, lefts);
        return RetiringRandomIterator.of(leftList, new Random(0));
    }

    @NullMarked
    private static final class SingleDrawIterator extends AbstractSingleDrawMoveIterator<TestdataSolution, String> {

        private final Map<String, Double> successChanceMap;
        private final Random random = new Random(0);
        private final Map<String, Integer> tryCountMap = new HashMap<>();
        private final Map<String, Integer> hasMoveCountMap = new HashMap<>();

        SingleDrawIterator(RetiringRandomIterator<@Nullable String> leftIterator, Map<String, Double> successChanceMap) {
            super(leftIterator);
            this.successChanceMap = successChanceMap;
        }

        @Override
        protected @Nullable Move<TestdataSolution> tryBuildMove(String left) {
            tryCountMap.merge(left, 1, Integer::sum);
            return random.nextDouble() < successChanceMap.get(left) ? new DummyMove(left) : null;
        }

        @Override
        protected boolean hasMove(String left) {
            hasMoveCountMap.merge(left, 1, Integer::sum);
            return successChanceMap.get(left) > 0.0;
        }

    }

}
