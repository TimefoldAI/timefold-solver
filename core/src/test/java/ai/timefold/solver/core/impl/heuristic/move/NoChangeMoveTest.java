package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class NoChangeMoveTest {

    @Test
    void isMoveDoable() {
        assertThat(NoChangeMove.getInstance().isMoveDoable(null)).isFalse();
    }

    @Test
    void rebase() {
        var destinationScoreDirector = mockRebasingScoreDirector(TestdataSolution.buildSolutionDescriptor(), new Object[][] {});
        var move = NoChangeMove.<TestdataSolution> getInstance();
        var rebasedMove = move.rebase(destinationScoreDirector);
        assertThat(rebasedMove).isSameAs(move);
    }

}
