package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class DefaultSolverFactoryTest {

    @Test
    void moveThreadCountAutoIsCorrectlyResolvedWhenCpuCountIsPositive() {
        assertThat(mockMoveThreadCountResolverAuto(1)).isNull();
        assertThat(mockMoveThreadCountResolverAuto(2)).isNull();
        assertThat(mockMoveThreadCountResolverAuto(4)).isEqualTo(2);
        assertThat(mockMoveThreadCountResolverAuto(5)).isEqualTo(3);
        assertThat(mockMoveThreadCountResolverAuto(6)).isEqualTo(4);
        assertThat(mockMoveThreadCountResolverAuto(100)).isEqualTo(4);
    }

    @Test
    void moveThreadCountAutoIsResolvedToNullWhenCpuCountIsNegative() {
        assertThat(mockMoveThreadCountResolverAuto(-1)).isNull();
    }

    private Integer mockMoveThreadCountResolverAuto(int mockCpuCount) {
        DefaultSolverFactory.MoveThreadCountResolver moveThreadCountResolverMock =
                new DefaultSolverFactory.MoveThreadCountResolver() {
                    @Override
                    protected int getAvailableProcessors() {
                        return mockCpuCount;
                    }
                };
        var maybeCount = moveThreadCountResolverMock.resolveMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_AUTO);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    @Test
    void moveThreadCountIsCorrectlyResolvedWhenValueIsPositive() {
        assertThat(resolveMoveThreadCount("2")).isEqualTo(2);
    }

    @Test
    void moveThreadCountThrowsExceptionWhenValueIsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> resolveMoveThreadCount("-1"));
    }

    @Test
    void moveThreadCountIsResolvedToNullWhenValueIsNone() {
        assertThat(resolveMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_NONE)).isNull();
    }

    private Integer resolveMoveThreadCount(String moveThreadCountString) {
        DefaultSolverFactory.MoveThreadCountResolver moveThreadCountResolver =
                new DefaultSolverFactory.MoveThreadCountResolver();
        var maybeCount = moveThreadCountResolver.resolveMoveThreadCount(moveThreadCountString);
        if (maybeCount.isPresent()) {
            return maybeCount.getAsInt();
        } else {
            return null;
        }
    }

    @Test
    void cachesScoreDirectorFactory() {
        SolverConfig solverConfig =
                SolverConfig.createFromXmlResource("ai/timefold/solver/core/config/solver/testdataSolverConfig.xml");
        DefaultSolverFactory<TestdataSolution> defaultSolverFactory = new DefaultSolverFactory<>(solverConfig);

        SolutionDescriptor<TestdataSolution> solutionDescriptor1 = defaultSolverFactory.getSolutionDescriptor();
        InnerScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory1 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor1).isNotNull();
            softly.assertThat(scoreDirectorFactory1).isNotNull();
            softly.assertThat(scoreDirectorFactory1.getSolutionDescriptor()).isSameAs(solutionDescriptor1);
        });

        SolutionDescriptor<TestdataSolution> solutionDescriptor2 = defaultSolverFactory.getSolutionDescriptor();
        InnerScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory2 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor2).isSameAs(solutionDescriptor1);
            softly.assertThat(scoreDirectorFactory2).isSameAs(scoreDirectorFactory1);
        });
    }

}
