package ai.timefold.solver.core.api.score;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractScoreTest {

    protected static void assertScoreNotFeasible(Score... scores) {
        for (Score score : scores) {
            assertThat(score.isFeasible()).as(score + " should not be feasible.").isFalse();
        }
    }

    protected static void assertScoreFeasible(Score... scores) {
        for (Score score : scores) {
            assertThat(score.isFeasible()).as(score + " should be feasible.").isTrue();
        }
    }

}
