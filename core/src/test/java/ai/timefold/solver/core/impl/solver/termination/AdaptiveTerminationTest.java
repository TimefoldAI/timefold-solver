package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

import org.junit.jupiter.api.Test;

class AdaptiveTerminationTest {

    @Test
    void testNoImprovementInGraceTerminates() {
        var termination = new AdaptiveTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, SimpleScore.ZERO);
        assertThat(termination.isTerminated(10, SimpleScore.ZERO)).isTrue();
    }

    @Test
    void testTerminatesWhenScoreDoesNotImprove() {
        var termination = new AdaptiveTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, SimpleScore.ZERO);
        assertThat(termination.isTerminated(10, SimpleScore.ONE)).isFalse();

        // Y_0 is 1 - 0 = 1

        var score = SimpleScore.of(2);
        termination.step(11, score);

        // These will compare as 2 - 1 = 1, 1 / 1 >= 1
        assertThat(termination.isTerminated(11, score)).isFalse();
        assertThat(termination.isTerminated(12, score)).isFalse();
        assertThat(termination.isTerminated(15, score)).isFalse();
        assertThat(termination.isTerminated(19, score)).isFalse();
        assertThat(termination.isTerminated(20, score)).isFalse();

        // This will compare as 2 - 2 = 0, 0 / 1 < 1, causing it to terminate
        assertThat(termination.isTerminated(21, score)).isTrue();
    }

    @Test
    void testTerminatesWhenImprovementDoesNotMeetCriteria() {
        var termination = new AdaptiveTermination<Object, SimpleScore>(10, 1);
        termination.start(0L, SimpleScore.ZERO);

        var score = SimpleScore.of(2);
        assertThat(termination.isTerminated(10, score)).isFalse();
        // Y_0 is 2 - 0 = 2

        score = SimpleScore.of(4);
        termination.step(11, score);

        // These will compare as 4 - 2 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(11, score)).isFalse();
        assertThat(termination.isTerminated(12, score)).isFalse();
        assertThat(termination.isTerminated(15, score)).isFalse();
        assertThat(termination.isTerminated(19, score)).isFalse();
        assertThat(termination.isTerminated(20, score)).isFalse();

        score = SimpleScore.of(6);
        termination.step(21, score);

        // These will compare as 6 - 4 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(21, score)).isFalse();
        assertThat(termination.isTerminated(30, score)).isFalse();

        score = SimpleScore.of(7);
        termination.step(21, score);

        // This will compare as 7 - 6 = 1, 1 / 2 < 1, so terminate
        assertThat(termination.isTerminated(31, score)).isTrue();
    }

    @Test
    void testImprovementInHardScoreResetsGrace() {
        var termination = new AdaptiveTermination<Object, HardSoftScore>(10, 1);

        var score = HardSoftScore.of(-1, 0);
        termination.start(0L, score);

        score = HardSoftScore.of(-1, 1);
        assertThat(termination.isTerminated(10, score)).isFalse();
        // Y_0 is 1 - 0 = 1

        score = HardSoftScore.of(-1, 2);
        termination.step(11, score);

        // These will compare as 2 - 1 = 1, 1 / 1 >= 1
        assertThat(termination.isTerminated(11, score)).isFalse();
        assertThat(termination.isTerminated(12, score)).isFalse();
        assertThat(termination.isTerminated(15, score)).isFalse();
        assertThat(termination.isTerminated(19, score)).isFalse();
        assertThat(termination.isTerminated(20, score)).isFalse();

        score = HardSoftScore.of(0, 2);
        termination.step(21, score);

        // This will reset the grace period
        assertThat(termination.isTerminated(21, score)).isFalse();
        assertThat(termination.isTerminated(30, score)).isFalse();

        score = HardSoftScore.of(0, 4);
        termination.step(31, score);

        // Y_0 is 4 - 2 = 2

        // These will compare as 4 - 2 = 2, 2 / 2 >= 1
        assertThat(termination.isTerminated(31, score)).isFalse();
        assertThat(termination.isTerminated(40, score)).isFalse();

        score = HardSoftScore.of(0, 5);
        termination.step(31, score);
        // This will compare as 5 - 4 = 1, 1 / 2 < 1, so terminate
        assertThat(termination.isTerminated(41, score)).isTrue();
    }

    @Test
    void testImprovementInHardScoreDuringGrace() {
        var termination = new AdaptiveTermination<Object, HardSoftScore>(10, 1);

        var score = HardSoftScore.of(-1, 0);
        termination.start(0, score);

        score = HardSoftScore.of(0, -1);
        termination.step(5, score);
        assertThat(termination.isTerminated(5, score)).isFalse();

        score = HardSoftScore.of(0, 0);
        termination.step(10, score);
        assertThat(termination.isTerminated(10, score)).isFalse();

        score = HardSoftScore.of(0, 1);
        termination.step(15, score);
        assertThat(termination.isTerminated(15, score)).isFalse();

        // Y_0 is 1 - (-1) = 2
        // These will compare as 1 - (-1) = 2, 2 / 2 >= 2
        assertThat(termination.isTerminated(16, score)).isFalse();
        assertThat(termination.isTerminated(19, score)).isFalse();

        // These will compare as 1 - 0 = 1, 1 / 2 < 2, so terminate
        assertThat(termination.isTerminated(20, score)).isTrue();
    }
}
