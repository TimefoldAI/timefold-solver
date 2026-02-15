package ai.timefold.solver.core.impl.heuristic.move;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SelectorBasedNoChangeMoveTest {

    @Test
    void isMoveDoable() {
        assertThat(SelectorBasedNoChangeMove.getInstance().isMoveDoable(null)).isFalse();
    }

    @Test
    void rebase() {
        var destinationScoreDirector = mockRebasingScoreDirector(TestdataSolution.buildSolutionDescriptor(), new Object[][] {});
        var move = SelectorBasedNoChangeMove.<TestdataSolution> getInstance();
        var rebasedMove = move.rebase(destinationScoreDirector.getMoveDirector());
        assertThat(rebasedMove).isSameAs(move);
    }

}
