package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

/**
 * Integration test demonstrating the use of ConstraintProvider instances.
 */
class ConstraintProviderInstanceIntegrationTest {

    @Test
    void solverWithConstraintProviderInstance() {
        // Create a custom constraint provider instance with runtime configuration
        var customConstraintProvider = new CustomizableConstraintProvider(true, 5);

        // Build solver config with the instance
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProvider(customConstraintProvider))
                .withTerminationConfig(
                        new TerminationConfig().withBestScoreLimit("0").withSpentLimit(Duration.ofSeconds(30)));

        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        // Prepare test data
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(List.of(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));

        // Solve and verify
        var solvedSolution = solver.solve(solution);
        assertThat(solvedSolution).isNotNull();
        assertThat(solvedSolution.getScore()).isNotNull();
    }

    @Test
    void solverWithDifferentConstraintProviderInstances() {
        // Test with strict configuration
        var strictProvider = new CustomizableConstraintProvider(true, 10);
        var strictSolver = createSolver(strictProvider);

        // Test with lenient configuration
        var lenientProvider = new CustomizableConstraintProvider(false, 1);
        var lenientSolver = createSolver(lenientProvider);

        // Both solvers should work with different constraint configurations
        var solution = createTestSolution();

        var strictResult = strictSolver.solve(solution);
        var lenientResult = lenientSolver.solve(solution);

        assertThat(strictResult.getScore()).isNotNull();
        assertThat(lenientResult.getScore()).isNotNull();

        // Strict provider penalizes more, so score should be worse (more negative)
        assertThat(strictResult.getScore().score()).isLessThanOrEqualTo(lenientResult.getScore().score());
    }

    private ai.timefold.solver.core.api.solver.Solver<TestdataSolution> createSolver(
            ConstraintProvider constraintProvider) {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProvider(constraintProvider))
                .withTerminationConfig(
                        new TerminationConfig().withBestScoreLimit("0").withSpentLimit(Duration.ofSeconds(30)));
        return SolverFactory.<TestdataSolution> create(solverConfig).buildSolver();
    }

    private TestdataSolution createTestSolution() {
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(List.of(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        return solution;
    }

    /**
     * Example of a configurable constraint provider that can be customized at
     * runtime.
     */
    public static class CustomizableConstraintProvider implements ConstraintProvider {

        private final boolean strictMode;
        private final int penaltyWeight;

        public CustomizableConstraintProvider(boolean strictMode, int penaltyWeight) {
            this.strictMode = strictMode;
            this.penaltyWeight = penaltyWeight;
        }

        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            if (strictMode) {
                return new Constraint[] {
                        strictConstraint(constraintFactory),
                        additionalConstraint(constraintFactory)
                };
            } else {
                return new Constraint[] {
                        strictConstraint(constraintFactory)
                };
            }
        }

        private Constraint strictConstraint(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataEntity.class)
                    .filter(entity -> entity.getValue() == null)
                    .penalize(SimpleScore.ONE.multiply(penaltyWeight))
                    .asConstraint("Strict constraint");
        }

        private Constraint additionalConstraint(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(TestdataEntity.class)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Additional constraint");
        }
    }
}
