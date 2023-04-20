package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class NoChangeMoveTest {

    @Test
    void isMoveDoable() {
        assertThat(new NoChangeMove<>().isMoveDoable(null)).isTrue();
    }

    @Test
    void createUndoMove() {
        assertThat(new NoChangeMove<>().createUndoMove(null))
                .isInstanceOf(NoChangeMove.class);
    }

    @Test
    void getPlanningEntities() {
        assertThat(new NoChangeMove<>().getPlanningEntities()).isEmpty();
    }

    @Test
    void getPlanningValues() {
        assertThat(new NoChangeMove<>().getPlanningValues()).isEmpty();
    }

    @Test
    void rebase() {
        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                TestdataSolution.buildSolutionDescriptor(), new Object[][] {});
        NoChangeMove<TestdataSolution> move = new NoChangeMove<>();
        move.rebase(destinationScoreDirector);
    }

}
