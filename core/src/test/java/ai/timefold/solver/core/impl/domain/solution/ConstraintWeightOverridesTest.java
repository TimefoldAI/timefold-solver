package ai.timefold.solver.core.impl.domain.solution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.constraintweightoverrides.TestdataConstraintWeightOverridesConstraintProvider;
import ai.timefold.solver.core.impl.testdata.domain.constraintweightoverrides.TestdataConstraintWeightOverridesSolution;
import ai.timefold.solver.core.impl.testdata.domain.constraintweightoverrides.TestdataExtendedConstraintWeightOverridesSolution;

import org.junit.jupiter.api.Test;

class ConstraintWeightOverridesTest {

    private static final String FIRST_WEIGHT = "First weight";
    private static final String SECOND_WEIGHT = "Second weight";
    private static final String THIRD_WEIGHT = "Third weight";

    private static final ConstraintRef FIRST_WEIGHT_REF =
            ConstraintRef.of(TestdataConstraintWeightOverridesSolution.class.getPackageName(), FIRST_WEIGHT);
    private static final ConstraintRef SECOND_WEIGHT_REF =
            ConstraintRef.of(TestdataConstraintWeightOverridesSolution.class.getPackageName(), SECOND_WEIGHT);

    @Test
    void consistentOrderOfConstraints() {
        var firstAndSecond = ConstraintWeightOverrides.of(Map.of(
                FIRST_WEIGHT, SimpleScore.ZERO,
                SECOND_WEIGHT, SimpleScore.ONE));
        var secondAndFirst = ConstraintWeightOverrides.of(Map.of(
                SECOND_WEIGHT, SimpleScore.ONE,
                FIRST_WEIGHT, SimpleScore.ZERO));
        assertThat(firstAndSecond.getKnownConstraintNames())
                .containsExactly(secondAndFirst.getKnownConstraintNames().toArray(new String[0]));
    }

    @Test
    void readsOverridesFromSolution() {
        var solutionDescriptor = TestdataConstraintWeightOverridesSolution.buildSolutionDescriptor();
        var constraintWeightSupplier = solutionDescriptor.getConstraintWeightSupplier();
        assertThat(constraintWeightSupplier).isNotNull();

        // Default weights
        var solution = new TestdataConstraintWeightOverridesSolution("solution");
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.none());
        assertThat(constraintWeightSupplier.getConstraintWeight(FIRST_WEIGHT_REF, solution))
                .isNull();
        assertThat(constraintWeightSupplier.getConstraintWeight(SECOND_WEIGHT_REF, solution))
                .isNull();

        // Override
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.of(Map.of(
                SECOND_WEIGHT, SimpleScore.ONE)));
        assertThat(constraintWeightSupplier.getConstraintWeight(FIRST_WEIGHT_REF, solution))
                .isNull();
        assertThat(constraintWeightSupplier.getConstraintWeight(SECOND_WEIGHT_REF, solution))
                .isEqualTo(SimpleScore.ONE);

        // Override to zero
        solution.setConstraintWeightOverrides(ConstraintWeightOverrides.of(Map.of(
                FIRST_WEIGHT, SimpleScore.ZERO,
                SECOND_WEIGHT, SimpleScore.ONE)));
        assertThat(constraintWeightSupplier.getConstraintWeight(FIRST_WEIGHT_REF, solution))
                .isEqualTo(SimpleScore.ZERO);
        assertThat(constraintWeightSupplier.getConstraintWeight(SECOND_WEIGHT_REF, solution))
                .isEqualTo(SimpleScore.ONE);
    }

    @Test
    void appliesOverridesToConstraintProvider() {
        var solutionDescriptor = TestdataConstraintWeightOverridesSolution.buildSolutionDescriptor();
        var solution = TestdataConstraintWeightOverridesSolution.generateSolution(3, 5);
        try (var scoreDirector = BavetConstraintStreamScoreDirectorFactory.buildScoreDirectorFactory(solutionDescriptor,
                new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(TestdataConstraintWeightOverridesConstraintProvider.class),
                EnvironmentMode.REPRODUCIBLE)
                .buildScoreDirector(false, false)) {
            // Default weights
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.triggerVariableListeners();
            assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(5));

            // Only second constraint is active
            solution.setConstraintWeightOverrides(ConstraintWeightOverrides.of(Map.of(
                    FIRST_WEIGHT, SimpleScore.ZERO,
                    SECOND_WEIGHT, SimpleScore.of(2))));
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.triggerVariableListeners();
            assertThat(scoreDirector.calculateScore()).isEqualTo(SimpleScore.of(10));

            // Unknown constraint is present
            solution.setConstraintWeightOverrides(ConstraintWeightOverrides.of(Map.of(
                    THIRD_WEIGHT, SimpleScore.ONE)));
            assertThatThrownBy(() -> scoreDirector.setWorkingSolution(solution))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(FIRST_WEIGHT)
                    .hasMessageContaining(SECOND_WEIGHT)
                    .hasMessageContaining(THIRD_WEIGHT);
        }
    }

    @Test
    void failsFastOnExtendedSolution() {
        assertThatThrownBy(TestdataExtendedConstraintWeightOverridesSolution::buildExtendedSolutionDescriptor)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already found");
    }

}
