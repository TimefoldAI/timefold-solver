package ai.timefold.solver.core.impl.domain.score.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ScoreDescriptorTest {

    @Test
    void scoreDefinition() {
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        ScoreDefinition<?> scoreDefinition = solutionDescriptor.getScoreDefinition();
        assertThat(scoreDefinition).isInstanceOf(SimpleScoreDefinition.class);
        assertThat(scoreDefinition.getScoreClass()).isEqualTo(SimpleScore.class);
    }

    @Test
    void scoreAccess() {
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        TestdataSolution solution = new TestdataSolution();

        assertThat((SimpleScore) solutionDescriptor.getScore(solution)).isNull();

        SimpleScore score = SimpleScore.of(-2);
        solutionDescriptor.setScore(solution, score);
        assertThat((SimpleScore) solutionDescriptor.getScore(solution)).isSameAs(score);
    }
}
