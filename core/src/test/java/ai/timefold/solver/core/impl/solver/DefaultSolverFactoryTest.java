package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.invalid.noentity.TestdataNoEntitySolution;

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
        ScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory1 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor1).isNotNull();
            softly.assertThat(scoreDirectorFactory1).isNotNull();
            softly.assertThat(scoreDirectorFactory1.getSolutionDescriptor()).isSameAs(solutionDescriptor1);
        });

        SolutionDescriptor<TestdataSolution> solutionDescriptor2 = defaultSolverFactory.getSolutionDescriptor();
        ScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory2 =
                defaultSolverFactory.getScoreDirectorFactory();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(solutionDescriptor2).isSameAs(solutionDescriptor1);
            softly.assertThat(scoreDirectorFactory2).isSameAs(scoreDirectorFactory1);
        });
    }

    @Test
    void testNoSolutionConfiguration() {
        SolverConfig solverConfig = new SolverConfig();
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("The solver configuration must have a solutionClass")
                .hasMessageContaining(
                        "If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already.");
    }

    @Test
    void testNoEntityConfiguration() {
        SolverConfig solverConfig = new SolverConfig();
        solverConfig.setSolutionClass(TestdataNoEntitySolution.class);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig))
                .hasMessageContaining("The solver configuration must have at least 1 entityClass")
                .hasMessageContaining(
                        "If you're using the Quarkus extension or Spring Boot starter, it should have been filled in already.");
    }

    @Test
    void testInvalidRandomConfiguration() {
        SolverConfig solverConfig =
                SolverConfig.createFromXmlResource("ai/timefold/solver/core/config/solver/testdataSolverConfig.xml")
                        .withRandomFactoryClass(RandomFactory.class)
                        .withRandomSeed(1000L);
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig).buildSolver(new SolverConfigOverride<>()))
                .hasMessageContaining("The solverConfig with randomFactoryClass ")
                .hasMessageContaining("has a non-null randomType (null) or a non-null randomSeed (1000).");
    }

    @Test
    void testInvalidMoveThreadCountConfiguration() {
        SolverConfig solverConfig =
                SolverConfig.createFromXmlResource("ai/timefold/solver/core/config/solver/testdataSolverConfig.xml")
                        .withMoveThreadCount("-1");
        assertThatCode(() -> new DefaultSolverFactory<>(solverConfig).buildSolver(new SolverConfigOverride<>()))
                .hasMessageContaining("The moveThreadCount")
                .hasMessageContaining("resulted in a resolvedMoveThreadCount")
                .hasMessageContaining("that is lower than 1.");
    }

}
