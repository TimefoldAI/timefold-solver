package ai.timefold.solver.core.impl.exhaustivesearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.exhaustivesearch.NodeExplorationType;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedOtherValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The test runs through all available configuration combinations related to ExhaustiveSearch and mixed models with two entities
 * and three values.
 * <p>
 * A solution state is represented by a string made up of four groups of characters,
 * two groups for the basic variables and two for the list variable.
 * The {@code -} character represents uninitialized entities.
 * For example, in the string {@code (11)(22)[3]-},
 * the first and second entities with basic variables are assigned the values {@code 1} and {@code 2}, respectively.
 * In addition, the first entity has the value {@code 3} assigned to its list variable,
 * while the second entity has no value assigned to its list variable.
 * <p>
 * In relation to the score, when the same value is assigned to different entities for the basic variables,
 * or when the value of the first variable is equal to the value of the second, the score will be penalized.
 * For list variables, the score decreases if any list contains an empty list or contains more than one value.
 */
class BlackBoxMixedVariableExhaustiveSearchPhaseTest {

    static Collection<Object[]> params() {
        return List.of(
                toObjectArray(
                        ExhaustiveSearchType.BRUTE_FORCE,
                        null,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("(--)(--)[-][-]", "(11)(--)[-][-]", "(12)(--)[-][-]", "(13)(--)[-][-]", "(21)(--)[-][-]",
                                "(22)(--)[-][-]", "(23)(--)[-][-]", "(31)(--)[-][-]", "(32)(--)[-][-]", "(33)(--)[-][-]",
                                "(11)(11)[-][-]", "(11)(12)[-][-]", "(11)(13)[-][-]", "(11)(21)[-][-]", "(11)(22)[-][-]",
                                "(11)(23)[-][-]", "(11)(31)[-][-]", "(11)(32)[-][-]", "(11)(33)[-][-]", "(12)(11)[-][-]",
                                "(12)(12)[-][-]", "(12)(13)[-][-]", "(12)(21)[-][-]", "(12)(22)[-][-]", "(12)(23)[-][-]",
                                "(12)(31)[-][-]", "(12)(32)[-][-]", "(12)(33)[-][-]", "(13)(11)[-][-]", "(13)(12)[-][-]",
                                "(13)(13)[-][-]", "(13)(21)[-][-]", "(13)(22)[-][-]", "(13)(23)[-][-]", "(13)(31)[-][-]",
                                "(13)(32)[-][-]", "(13)(33)[-][-]", "(21)(11)[-][-]", "(21)(12)[-][-]", "(21)(13)[-][-]",
                                "(21)(21)[-][-]", "(21)(22)[-][-]", "(21)(23)[-][-]", "(21)(31)[-][-]", "(21)(32)[-][-]",
                                "(21)(33)[-][-]", "(22)(11)[-][-]", "(22)(12)[-][-]", "(22)(13)[-][-]", "(22)(21)[-][-]",
                                "(22)(22)[-][-]", "(22)(23)[-][-]", "(22)(31)[-][-]", "(22)(32)[-][-]", "(22)(33)[-][-]",
                                "(23)(11)[-][-]", "(23)(12)[-][-]", "(23)(13)[-][-]", "(23)(21)[-][-]", "(23)(22)[-][-]",
                                "(23)(23)[-][-]", "(23)(31)[-][-]", "(23)(32)[-][-]", "(23)(33)[-][-]", "(31)(11)[-][-]",
                                "(31)(12)[-][-]", "(31)(13)[-][-]", "(31)(21)[-][-]", "(31)(22)[-][-]", "(31)(23)[-][-]",
                                "(31)(31)[-][-]", "(31)(32)[-][-]", "(31)(33)[-][-]", "(32)(11)[-][-]", "(32)(12)[-][-]",
                                "(32)(13)[-][-]", "(32)(21)[-][-]", "(32)(22)[-][-]", "(32)(23)[-][-]", "(32)(31)[-][-]",
                                "(32)(32)[-][-]", "(32)(33)[-][-]", "(33)(11)[-][-]", "(33)(12)[-][-]", "(33)(13)[-][-]",
                                "(33)(21)[-][-]", "(33)(22)[-][-]", "(33)(23)[-][-]", "(33)(31)[-][-]", "(33)(32)[-][-]",
                                "(33)(33)[-][-]", // Finished basic variable
                                "(12)(21)[4][-]", "(12)(21)[5][-]", "(12)(21)[6][-]", "(12)(21)[-][4]", "(12)(21)[-][5]",
                                "(12)(21)[-][6]", "(12)(21)[-][4]", // Repeated values indicate the process of restoring the solution
                                "(12)(21)[-][4,5]", "(12)(21)[-][4,6]", "(12)(21)[-][5]", "(12)(21)[-][5,4]",
                                "(12)(21)[-][5,6]", "(12)(21)[-][6]", "(12)(21)[-][6,4]", "(12)(21)[-][6,5]",
                                "(12)(21)[-][4,5]", "(12)(21)[-][4,5,6]", "(12)(21)[-][4,6]", "(12)(21)[-][4,6,5]",
                                "(12)(21)[-][5,4]", "(12)(21)[-][5,4,6]", "(12)(21)[-][5,6]", "(12)(21)[-][5,6,4]",
                                "(12)(21)[-][6,4]", "(12)(21)[-][6,4,5]", "(12)(21)[-][6,5]", "(12)(21)[-][6,5,4]",
                                "(12)(21)[-][-]", "(12)(21)[4][-]", "(12)(21)[5][-]", "(12)(21)[6][-]", "(12)(21)[-][4]",
                                "(12)(21)[-][5]", "(12)(21)[-][6]", "(12)(21)[-][4]", "(12)(21)[-][4,5]", "(12)(21)[-][4,6]",
                                "(12)(21)[-][5]", "(12)(21)[-][5,4]", "(12)(21)[-][5,6]", "(12)(21)[-][6]", "(12)(21)[-][6,4]",
                                "(12)(21)[-][6,5]", "(12)(21)[-][4,5]", "(12)(21)[-][4,5,6]", "(12)(21)[-][4,6]",
                                "(12)(21)[-][4,6,5]", "(12)(21)[-][5,4]", "(12)(21)[-][5,4,6]", "(12)(21)[-][5,6]",
                                "(12)(21)[-][5,6,4]", "(12)(21)[-][6,4]", "(12)(21)[-][6,4,5]", "(12)(21)[-][6,5]",
                                "(12)(21)[-][6,5,4]", "(12)(21)[4][-]", "(12)(21)[4,5][-]", "(12)(21)[4,6][-]",
                                "(12)(21)[4][5]", "(12)(21)[4][6]", "(12)(21)[4][5]", "(12)(21)[4][5,6]", "(12)(21)[4][6]",
                                "(12)(21)[4][6,5]", "(12)(21)[5][-]", "(12)(21)[5,4][-]", "(12)(21)[5,6][-]", "(12)(21)[5][4]",
                                "(12)(21)[5][6]", "(12)(21)[5][4]", "(12)(21)[5][4,6]", "(12)(21)[5][6]", "(12)(21)[5][6,4]",
                                "(12)(21)[6][-]", "(12)(21)[6,4][-]", "(12)(21)[6,5][-]", "(12)(21)[6][4]", "(12)(21)[6][5]",
                                "(12)(21)[6][-]", "(12)(21)[6][-]")),
                toObjectArray(ExhaustiveSearchType.BRANCH_AND_BOUND,
                        NodeExplorationType.DEPTH_FIRST,
                        EntitySorterManner.NONE,
                        ValueSorterManner.NONE,
                        List.of("(--)(--)[-][-]", "(--)(--)[-][-]", "(11)(--)[-][-]", "(12)(--)[-][-]", "(13)(--)[-][-]",
                                "(21)(--)[-][-]", "(22)(--)[-][-]", "(23)(--)[-][-]", "(31)(--)[-][-]", "(32)(--)[-][-]",
                                "(33)(--)[-][-]", "(12)(11)[-][-]", "(12)(12)[-][-]", "(12)(13)[-][-]", "(12)(21)[-][-]",
                                "(12)(22)[-][-]", "(12)(23)[-][-]", "(12)(31)[-][-]", "(12)(32)[-][-]", "(12)(33)[-][-]",
                                "(13)(11)[-][-]", "(13)(12)[-][-]", "(13)(13)[-][-]", "(13)(21)[-][-]", "(13)(22)[-][-]",
                                "(13)(23)[-][-]", "(13)(31)[-][-]", "(13)(32)[-][-]", "(13)(33)[-][-]", "(21)(11)[-][-]",
                                "(21)(12)[-][-]", "(21)(13)[-][-]", "(21)(21)[-][-]", "(21)(22)[-][-]", "(21)(23)[-][-]",
                                "(21)(31)[-][-]", "(21)(32)[-][-]", "(21)(33)[-][-]", "(23)(11)[-][-]", "(23)(12)[-][-]",
                                "(23)(13)[-][-]", "(23)(21)[-][-]", "(23)(22)[-][-]", "(23)(23)[-][-]", "(23)(31)[-][-]",
                                "(23)(32)[-][-]", "(23)(33)[-][-]", "(31)(11)[-][-]", "(31)(12)[-][-]", "(31)(13)[-][-]",
                                "(31)(21)[-][-]", "(31)(22)[-][-]", "(31)(23)[-][-]", "(31)(31)[-][-]", "(31)(32)[-][-]",
                                "(31)(33)[-][-]", "(32)(11)[-][-]", "(32)(12)[-][-]", "(32)(13)[-][-]", "(32)(21)[-][-]",
                                "(32)(22)[-][-]", "(32)(23)[-][-]", "(32)(31)[-][-]", "(32)(32)[-][-]", "(32)(33)[-][-]",
                                "(11)(11)[-][-]", "(11)(12)[-][-]", "(11)(13)[-][-]", "(11)(21)[-][-]", "(11)(22)[-][-]",
                                "(11)(23)[-][-]", "(11)(31)[-][-]", "(11)(32)[-][-]", "(11)(33)[-][-]", "(22)(11)[-][-]",
                                "(22)(12)[-][-]", "(22)(13)[-][-]", "(22)(21)[-][-]", "(22)(22)[-][-]", "(22)(23)[-][-]",
                                "(22)(31)[-][-]", "(22)(32)[-][-]", "(22)(33)[-][-]", "(33)(11)[-][-]", "(33)(12)[-][-]",
                                "(33)(13)[-][-]", "(33)(21)[-][-]", "(33)(22)[-][-]", "(33)(23)[-][-]", "(33)(31)[-][-]",
                                "(33)(32)[-][-]", "(33)(33)[-][-]", // Finished basic variable
                                "(12)(21)[-][-]", "(12)(21)[-][-]", "(12)(21)[4][-]", "(12)(21)[5][-]", "(12)(21)[6][-]",
                                "(12)(21)[-][4]", "(12)(21)[-][5]", "(12)(21)[-][6]", "(12)(21)[-][4]", // Repeated values indicate the process of restoring the solution
                                "(12)(21)[-][4,5]", "(12)(21)[-][4,6]", "(12)(21)[-][5]", "(12)(21)[-][5,4]",
                                "(12)(21)[-][5,6]", "(12)(21)[-][6]", "(12)(21)[-][6,4]", "(12)(21)[-][6,5]",
                                "(12)(21)[-][6,4]", "(12)(21)[-][6,4,5]", "(12)(21)[4][-]", "(12)(21)[4,5][-]",
                                "(12)(21)[4,6][-]", "(12)(21)[4][5]", "(12)(21)[4][6]", "(12)(21)[4][5]", "(12)(21)[4][5,6]",
                                "(12)(21)[4][6]", "(12)(21)[4][6,5]", "(12)(21)[5][-]", "(12)(21)[5,4][-]", "(12)(21)[5,6][-]",
                                "(12)(21)[5][4]", "(12)(21)[5][6]", "(12)(21)[5][4]", "(12)(21)[5][4,6]", "(12)(21)[5][6]",
                                "(12)(21)[5][6,4]", "(12)(21)[6][-]", "(12)(21)[6,4][-]", "(12)(21)[6,5][-]", "(12)(21)[6][4]",
                                "(12)(21)[6][5]", "(12)(21)[6][4]", "(12)(21)[6][4,5]", "(12)(21)[6][5]", "(12)(21)[6][5,4]",
                                "(12)(21)[-][-]", "(12)(21)[4][-]", "(12)(21)[5][-]", "(12)(21)[6][-]", "(12)(21)[-][4]",
                                "(12)(21)[-][5]", "(12)(21)[-][6]", "(12)(21)[-][4]", "(12)(21)[-][4,5]", "(12)(21)[-][4,6]",
                                "(12)(21)[-][5]", "(12)(21)[-][5,4]", "(12)(21)[-][5,6]", "(12)(21)[-][6]", "(12)(21)[-][6,4]",
                                "(12)(21)[-][6,5]", "(12)(21)[4][-]", "(12)(21)[4,5][-]", "(12)(21)[4,6][-]", "(12)(21)[4][5]",
                                "(12)(21)[4][6]", "(12)(21)[4][5]", "(12)(21)[4][5,6]", "(12)(21)[4][6]", "(12)(21)[4][6,5]",
                                "(12)(21)[5][-]", "(12)(21)[5,4][-]", "(12)(21)[5,6][-]", "(12)(21)[5][4]", "(12)(21)[5][6]",
                                "(12)(21)[5][4]", "(12)(21)[5][4,6]", "(12)(21)[5][6]", "(12)(21)[5][6,4]", "(12)(21)[6][-]",
                                "(12)(21)[6,4][-]", "(12)(21)[6,5][-]", "(12)(21)[6][4]", "(12)(21)[6][5]", "(12)(21)[6][4]",
                                "(12)(21)[6][4,5]", "(12)(21)[6][5]", "(12)(21)[6][5,4]", "(12)(21)[6][5]", "(12)(21)[6][5]")));
    }

    private static Object[] toObjectArray(Object... parameters) {
        return parameters;
    }

    private static SolverConfig buildSolverConfig(
            EntitySorterManner entitySorterManner, ValueSorterManner valueSorterManner,
            ExhaustiveSearchType exhaustiveSearchType, NodeExplorationType nodeExplorationType) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataMixedSolution.class, TestdataMixedEntity.class,
                TestdataMixedOtherValue.class, TestdataMixedValue.class);

        ExhaustiveSearchPhaseConfig exhaustiveSearchPhaseConfig = new ExhaustiveSearchPhaseConfig()
                .withExhaustiveSearchType(exhaustiveSearchType)
                .withNodeExplorationType(nodeExplorationType)
                .withEntitySorterManner(entitySorterManner)
                .withValueSorterManner(valueSorterManner)
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(37));

        solverConfig.setPhaseConfigList(Collections.singletonList(exhaustiveSearchPhaseConfig));
        solverConfig.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(TestdataComparableMixedCalculator.class)
                .withInitializingScoreTrend("ONLY_DOWN"));

        return solverConfig;
    }

    private static TestdataMixedSolution buildSolution() {
        var solution = new TestdataMixedSolution();
        solution.setEntityList(List.of(new TestdataMixedEntity("entity1", 1),
                new TestdataMixedEntity("entity2", 2)));
        solution.setValueList(List.of(new TestdataMixedValue("4"),
                new TestdataMixedValue("5"),
                new TestdataMixedValue("6")));
        solution.setOtherValueList(List.of(new TestdataMixedOtherValue("1", 1),
                new TestdataMixedOtherValue("2", 2),
                new TestdataMixedOtherValue("3", 3)));
        return solution;
    }

    @ParameterizedTest(name = "{0}, NodeExplorationType-{1}, EntitySorterManner-{2}, ValueSorterManner-{3}")
    @MethodSource("params")
    void verifyExhaustiveSearchSteps(
            ExhaustiveSearchType exhaustiveSearchType,
            NodeExplorationType nodeExplorationType,
            EntitySorterManner entitySorterManner,
            ValueSorterManner valueSorterManner,
            List<String> steps) {
        var solverConfig = buildSolverConfig(
                entitySorterManner,
                valueSorterManner,
                exhaustiveSearchType,
                nodeExplorationType);
        var solverFactory = SolverFactory.<TestdataMixedSolution> create(solverConfig);

        if (exhaustiveSearchType == ExhaustiveSearchType.BRUTE_FORCE && nodeExplorationType != null) {
            assertThatIllegalArgumentException()
                    .isThrownBy(solverFactory::buildSolver)
                    .withMessage("The phaseConfig (ExhaustiveSearchPhaseConfig) has an "
                            + "nodeExplorationType (" + nodeExplorationType.name()
                            + ") which is not compatible with its exhaustiveSearchType (BRUTE_FORCE).");
        } else {
            var solver = solverFactory.buildSolver();
            WorkingSolutionTracker.get().reset();
            solver.solve(buildSolution());
            assertThat(WorkingSolutionTracker.get().getWorkingSolutions()).containsExactlyElementsOf(steps);
        }
    }

    public static class WorkingSolutionTracker {

        private static WorkingSolutionTracker INSTANCE = null;
        private final List<String> workingSolutions = new ArrayList<>();

        public static WorkingSolutionTracker get() {
            if (INSTANCE == null) {
                INSTANCE = new WorkingSolutionTracker();
            }
            return INSTANCE;
        }

        private WorkingSolutionTracker() {
        }

        public void reset() {
            workingSolutions.clear();
        }

        public void addWorkingSolution(TestdataMixedSolution solution) {
            var basicVariableNotation = new StringBuilder();
            var listVariableNotation = new StringBuilder();
            for (var entity : solution.getEntityList().stream().sorted(Comparator.comparing(TestdataObject::getCode))
                    .toList()) {
                basicVariableNotation.append("(");
                if (entity.getBasicValue() != null) {
                    basicVariableNotation.append(entity.getBasicValue().getCode());
                } else {
                    basicVariableNotation.append("-");
                }
                if (entity.getSecondBasicValue() != null) {
                    basicVariableNotation.append(entity.getSecondBasicValue().getCode());
                } else {
                    basicVariableNotation.append("-");
                }
                basicVariableNotation.append(")");
                listVariableNotation.append("[");
                listVariableNotation.append(entity.getValueList().isEmpty() ? "-"
                        : entity.getValueList().stream().map(TestdataObject::getCode).collect(Collectors.joining(",")));
                listVariableNotation.append("]");
            }
            workingSolutions.add(basicVariableNotation.append(listVariableNotation).toString());
        }

        public List<String> getWorkingSolutions() {
            return workingSolutions;
        }

    }

    public static class TestdataComparableMixedCalculator
            implements EasyScoreCalculator<TestdataMixedSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataMixedSolution solution) {
            WorkingSolutionTracker.get().addWorkingSolution(solution);
            var score = 0;
            var alreadyUsedValues = new HashSet<TestdataMixedOtherValue>();
            for (var entity : solution.getEntityList()) {
                var size = entity.getValueList().size();
                // List variable
                if (size == 0) {
                    score--;
                } else if (size > 1) {
                    score -= 5;
                }
                if (entity.getBasicValue() == entity.getSecondBasicValue()) {
                    score--;
                }
                if (entity.getBasicValue() != null) {
                    if (alreadyUsedValues.contains(entity.getBasicValue())) {
                        score -= 5;
                    }
                    alreadyUsedValues.add(entity.getBasicValue());
                }
            }
            return SimpleScore.of(score);
        }
    }
}
