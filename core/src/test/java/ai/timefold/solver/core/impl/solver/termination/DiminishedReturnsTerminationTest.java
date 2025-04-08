package ai.timefold.solver.core.impl.solver.termination;

import static ai.timefold.solver.core.impl.solver.termination.DiminishedReturnsTermination.NANOS_PER_MILLISECOND;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class DiminishedReturnsTerminationTest {

    @Test
    void testNoImprovementInGraceTerminates() {
        var termination = new DiminishedReturnsTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, InnerScore.fullyAssigned(SimpleScore.ZERO));
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, InnerScore.fullyAssigned(SimpleScore.ZERO))).isTrue();
    }

    @Test
    void testTerminatesWhenScoreDoesNotImprove() {
        var termination = new DiminishedReturnsTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, InnerScore.fullyAssigned(SimpleScore.ZERO));
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, InnerScore.fullyAssigned(SimpleScore.ONE))).isFalse();

        // Y_0 is 1 - 0 = 1
        var score = InnerScore.fullyAssigned(SimpleScore.of(2));
        termination.step(11 * NANOS_PER_MILLISECOND, score);

        // These will compare as 2 - 1 = 1, 1 / 1 >= 1
        assertThat(termination.isTerminated(11 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(12 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isFalse();

        // This will compare as 2 - 2 = 0, 0 / 1 < 1, causing it to terminate
        assertThat(termination.isTerminated(21 * NANOS_PER_MILLISECOND, score)).isTrue();

        // No improvement in the score
        termination.start(0L, InnerScore.fullyAssigned(SimpleScore.ONE));
        termination.step(NANOS_PER_MILLISECOND, InnerScore.fullyAssigned(SimpleScore.ONE));
        // End the grace period
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, InnerScore.fullyAssigned(SimpleScore.ONE))).isTrue();
        // Second call bypass the time window verification and must be consistent
        assertThat(termination.isTerminated(11 * NANOS_PER_MILLISECOND, InnerScore.fullyAssigned(SimpleScore.ONE))).isTrue();
    }

    @Test
    void testTerminatesWhenImprovementDoesNotMeetCriteria() {
        var termination = new DiminishedReturnsTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, InnerScore.fullyAssigned(SimpleScore.ZERO));

        var score = InnerScore.fullyAssigned(SimpleScore.of(2));
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, score)).isFalse();
        // Y_0 is 2 - 0 = 2

        score = InnerScore.fullyAssigned(SimpleScore.of(4));
        termination.step(11 * NANOS_PER_MILLISECOND, score);

        // These will compare as 4 - 2 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(11 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(12 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.of(6));
        termination.step(21 * NANOS_PER_MILLISECOND, score);

        // These will compare as 6 - 4 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(21 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(30 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.of(7));
        termination.step(21 * NANOS_PER_MILLISECOND, score);

        // This will compare as 7 - 6 = 1, 1 / 2 < 1, so terminate
        assertThat(termination.isTerminated(31 * NANOS_PER_MILLISECOND, score)).isTrue();
    }

    @Test
    void testImprovementInInitScoreResetsGrace() {
        var termination = new DiminishedReturnsTermination<Object, SimpleScore>(10, 1);

        var score = InnerScore.withUnassignedCount(SimpleScore.of(0), 1);
        termination.start(0L, score);

        score = InnerScore.withUnassignedCount(SimpleScore.of(1), 1);
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, score)).isFalse();
        // Y_0 is 1 - 0 = 1

        score = InnerScore.withUnassignedCount(SimpleScore.of(2), 1);
        termination.step(11 * NANOS_PER_MILLISECOND, score);

        // These will compare as 2 - 1 = 1, 1 / 1 >= 1
        assertThat(termination.isTerminated(11 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(12 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.of(2));
        termination.step(21 * NANOS_PER_MILLISECOND, score);

        // This will reset the grace period
        assertThat(termination.isTerminated(21 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(30 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.of(4));
        termination.step(31 * NANOS_PER_MILLISECOND, score);

        // Y_0 is 4 - 2 = 2

        // These will compare as 4 - 2 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(31 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(40 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.of(5));
        termination.step(31 * NANOS_PER_MILLISECOND, score);
        // This will compare as 5 - 4 = 1, 1 / 2 < 1, so terminate
        assertThat(termination.isTerminated(41 * NANOS_PER_MILLISECOND, score)).isTrue();
    }

    @Test
    void testImprovementInHardScoreResetsGrace() {
        var termination = new DiminishedReturnsTermination<Object, HardSoftScore>(10, 1);

        var score = InnerScore.fullyAssigned(HardSoftScore.of(-1, 0));
        termination.start(0L, score);

        score = InnerScore.fullyAssigned(HardSoftScore.of(-1, 1));
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, score)).isFalse();
        // Y_0 is 1 - 0 = 1

        score = InnerScore.fullyAssigned(HardSoftScore.of(-1, 2));
        termination.step(11 * NANOS_PER_MILLISECOND, score);

        // These will compare as 2 - 1 = 1, 1 / 1 >= 1
        assertThat(termination.isTerminated(11 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(12 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, 2));
        termination.step(21 * NANOS_PER_MILLISECOND, score);

        // This will reset the grace period
        assertThat(termination.isTerminated(21 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(30 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, 4));
        termination.step(31 * NANOS_PER_MILLISECOND, score);

        // Y_0 is 4 - 2 = 2

        // These will compare as 4 - 2 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(31 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(40 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, 5));
        termination.step(31 * NANOS_PER_MILLISECOND, score);
        // This will compare as 5 - 4 = 1, 1 / 2 < 1, so terminate
        assertThat(termination.isTerminated(41 * NANOS_PER_MILLISECOND, score)).isTrue();
    }

    @Test
    void testImprovementInHardScoreDuringGrace() {
        var termination = new DiminishedReturnsTermination<Object, HardSoftScore>(10, 1);

        var score = InnerScore.fullyAssigned(HardSoftScore.of(-1, 0));
        termination.start(0, score);

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, -1));
        termination.step(5 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(5 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, 0));
        termination.step(10 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(HardSoftScore.of(0, 1));
        termination.step(15 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();

        // Y_0 is 1 - (-1) = 2
        // These will compare as 1 - (-1) = 2, 2 / 2 >= 2
        assertThat(termination.isTerminated(16 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();

        // These will compare as 1 - 0 = 1, 1 / 2 < 2, so terminate
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isTrue();
    }

    @Test
    void testImprovementInInitScoreDuringGrace() {
        var termination = new DiminishedReturnsTermination<Object, SimpleScore>(10, 1);

        var score = InnerScore.withUnassignedCount(SimpleScore.ZERO, 1);
        termination.start(0, score);

        score = InnerScore.fullyAssigned(SimpleScore.of(-1));
        termination.step(5 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(5 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.ZERO);
        termination.step(10 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(10 * NANOS_PER_MILLISECOND, score)).isFalse();

        score = InnerScore.fullyAssigned(SimpleScore.ONE);
        termination.step(15 * NANOS_PER_MILLISECOND, score);
        assertThat(termination.isTerminated(15 * NANOS_PER_MILLISECOND, score)).isFalse();

        // Y_0 is 1 - (-1) = 2
        // These will compare as 1 - (-1) = 2, 2 / 2 >= 2
        assertThat(termination.isTerminated(16 * NANOS_PER_MILLISECOND, score)).isFalse();
        assertThat(termination.isTerminated(19 * NANOS_PER_MILLISECOND, score)).isFalse();

        // These will compare as 1 - 0 = 1, 1 / 2 < 2, so terminate
        assertThat(termination.isTerminated(20 * NANOS_PER_MILLISECOND, score)).isTrue();
    }
}
