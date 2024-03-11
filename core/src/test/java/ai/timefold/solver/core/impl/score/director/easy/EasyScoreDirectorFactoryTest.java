package ai.timefold.solver.core.impl.score.director.easy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class EasyScoreDirectorFactoryTest {

    @Test
    void buildScoreDirector() {
        SolutionDescriptor<TestdataSolution> solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        EasyScoreCalculator<TestdataSolution, SimpleScore> scoreCalculator = mock(EasyScoreCalculator.class);
        when(scoreCalculator.calculateScore(any(TestdataSolution.class)))
                .thenAnswer(invocation -> SimpleScore.of(-10));
        EasyScoreDirectorFactory<TestdataSolution, SimpleScore> directorFactory = new EasyScoreDirectorFactory<>(
                solutionDescriptor, scoreCalculator);

        try (var director = directorFactory.buildScoreDirector(false, false)) {
            TestdataSolution solution = new TestdataSolution();
            solution.setValueList(Collections.emptyList());
            solution.setEntityList(Collections.emptyList());
            director.setWorkingSolution(solution);
            assertThat(director.calculateScore())
                    .isEqualTo(SimpleScore.ofUninitialized(0, -10));
        }
    }

}
