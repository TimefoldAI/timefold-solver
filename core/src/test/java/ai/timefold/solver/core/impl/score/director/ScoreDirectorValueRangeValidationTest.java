package ai.timefold.solver.core.impl.score.director;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoreDirectorValueRangeValidationTest {

    private static final String OUTSIDE_OF_VALUE_RANGE_MESSAGE = "outside of the related value range";

    // ************************************************************************
    // Helper methods
    // ************************************************************************

    private static <Solution_> AbstractScoreDirector<Solution_, SimpleScore, ?> buildScoreDirector(
            SolutionDescriptor<Solution_> solutionDescriptor, boolean lookUpEnabled, boolean valueRangeValidationEnabled) {
        var scoreDirectorFactory =
                new EasyScoreDirectorFactory<>(solutionDescriptor, solution -> SimpleScore.ZERO, EnvironmentMode.NO_ASSERT);
        return scoreDirectorFactory.createScoreDirectorBuilder()
                .withLookUpEnabled(lookUpEnabled)
                .withValueRangeValidationEnabled(valueRangeValidationEnabled)
                .build();
    }

    /**
     * The value range is [v1, v2, v3], but the list of the entity a is [v1, x].
     * The values v2 and v3 are unassigned, yet counting the values gives 3 - 2 = 1.
     */
    private static TestdataListSolution generateListSolutionWithValueOutsideOfValueRange() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var x = new TestdataListValue("x");
        var solution = new TestdataListSolution();
        solution.setValueList(new ArrayList<>(List.of(v1, v2, v3)));
        solution.setEntityList(new ArrayList<>(List.of(new TestdataListEntity("a", v1, x), new TestdataListEntity("b"))));
        return solution;
    }

    private static TestdataSolution generateBasicSolutionWithValueOutsideOfValueRange() {
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().get(0).setValue(new TestdataValue("x"));
        return solution;
    }

    // ************************************************************************
    // Validation of the working solution
    // ************************************************************************

    @Test
    void setWorkingSolutionFailsOnListValueOutsideOfValueRange() {
        var solution = generateListSolutionWithValueOutsideOfValueRange();
        try (var scoreDirector = buildScoreDirector(TestdataListSolution.buildSolutionDescriptor(), false, true)) {
            assertThatThrownBy(() -> scoreDirector.setWorkingSolution(solution))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("The value (x)")
                    .hasMessageContaining(OUTSIDE_OF_VALUE_RANGE_MESSAGE);
        }
    }

    @Test
    void setWorkingSolutionFailsOnBasicValueOutsideOfValueRange() {
        var solution = generateBasicSolutionWithValueOutsideOfValueRange();
        try (var scoreDirector = buildScoreDirector(TestdataSolution.buildSolutionDescriptor(), false, true)) {
            assertThatThrownBy(() -> scoreDirector.setWorkingSolution(solution))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("The value (x)")
                    .hasMessageContaining(OUTSIDE_OF_VALUE_RANGE_MESSAGE);
        }
    }

    @Test
    void setWorkingSolutionDoesNotValidateWhenDisabled() {
        var solution = generateListSolutionWithValueOutsideOfValueRange();
        try (var scoreDirector = buildScoreDirector(TestdataListSolution.buildSolutionDescriptor(), false, false)) {
            scoreDirector.setWorkingSolution(solution);
            var listVariableState =
                    scoreDirector.getListVariableState(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
            // The counts are wrong without any error: v2 and v3 are unassigned.
            assertThat(scoreDirector.getWorkingInitScore()).isEqualTo(-1);
            assertThat(listVariableState.getUnassignedCount()).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false, false",
            "false, true",
            "true, false",
            "true, true" })
    void validationDoesNotDependOnLookUp(boolean lookUpEnabled, boolean valueRangeValidationEnabled) {
        var solution = generateBasicSolutionWithValueOutsideOfValueRange();
        try (var scoreDirector =
                buildScoreDirector(TestdataSolution.buildSolutionDescriptor(), lookUpEnabled, valueRangeValidationEnabled)) {
            if (valueRangeValidationEnabled) {
                assertThatThrownBy(() -> scoreDirector.setWorkingSolution(solution))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(OUTSIDE_OF_VALUE_RANGE_MESSAGE);
            } else {
                assertThatCode(() -> scoreDirector.setWorkingSolution(solution))
                        .doesNotThrowAnyException();
            }
        }
    }

    // ************************************************************************
    // Child thread score directors
    // ************************************************************************

    @ParameterizedTest
    @CsvSource({ "false", "true" })
    void partThreadScoreDirectorKeepsTheValidationOfTheParent(boolean valueRangeValidationEnabled) {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        try (var scoreDirector = buildScoreDirector(solutionDescriptor, true, valueRangeValidationEnabled);
                var childScoreDirector = scoreDirector.createChildThreadScoreDirector(ChildThreadType.PART_THREAD)) {
            var solution = generateBasicSolutionWithValueOutsideOfValueRange();
            if (valueRangeValidationEnabled) {
                assertThatThrownBy(() -> childScoreDirector.setWorkingSolution(solution))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(OUTSIDE_OF_VALUE_RANGE_MESSAGE);
            } else {
                assertThatCode(() -> childScoreDirector.setWorkingSolution(solution))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Test
    void moveThreadScoreDirectorDoesNotValidate() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        try (var scoreDirector = buildScoreDirector(solutionDescriptor, true, false)) {
            scoreDirector.setWorkingSolution(generateBasicSolutionWithValueOutsideOfValueRange());
            // The move thread works on a clone of the parent's working solution, which the parent validates.
            assertThatCode(() -> scoreDirector.createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD).close())
                    .doesNotThrowAnyException();
        }
    }

    // ************************************************************************
    // Unassigned count of the list variable
    // ************************************************************************

    @Test
    void unassignedCountFailsWhenNegative() {
        // The value range is [v1], but the list is [v1, x, y].
        // Unassigned values are allowed, so the init score doesn't catch it.
        var v1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setValueList(new ArrayList<>(List.of(v1)));
        solution.setEntityList(new ArrayList<>(List.of(new TestdataAllowsUnassignedValuesListEntity("a", v1,
                new TestdataAllowsUnassignedValuesListValue("x"), new TestdataAllowsUnassignedValuesListValue("y")))));
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        try (var scoreDirector = buildScoreDirector(solutionDescriptor, false, false)) {
            scoreDirector.setWorkingSolution(solution);
            var listVariableState = scoreDirector.getListVariableState(solutionDescriptor.getListVariableDescriptor());
            assertThatThrownBy(listVariableState::getUnassignedCount)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("more assigned values (3) than values in its value range (1)");
        }
    }

}
