package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class NoChangeMoveTest {

    @Test
    void isMoveDoable() {
        assertThat(NoChangeMove.getInstance().isMoveDoable(null)).isFalse();
    }

    @Test
    void rebase() {
        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                TestdataSolution.buildSolutionDescriptor(), new Object[][] {});
        NoChangeMove<TestdataSolution> move = NoChangeMove.getInstance();
        assertThatThrownBy(() -> move.rebase(destinationScoreDirector))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
