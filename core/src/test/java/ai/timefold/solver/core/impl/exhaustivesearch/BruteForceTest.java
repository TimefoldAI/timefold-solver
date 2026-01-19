package ai.timefold.solver.core.impl.exhaustivesearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchType;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.TestdataListVarEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedEntityEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntityFirstEntity;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySecondEntity;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedOtherValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.junit.jupiter.api.Test;

class BruteForceTest {

    @Test
    void solveBasicVariableNotAllowedUnassigned() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));
        var solver = (DefaultSolver<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataSolution.generateSolution(2, 2);
        for (TestdataEntity entity : solution.getEntityList()) { // Make sure nothing is set.
            entity.setValue(null);
        }

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {

            @Override
            public void stepStarted(AbstractStepScope<TestdataSolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataSolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 3) {
                        fail("The exhaustive search phase was not ended after 3 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);

        assertSoftly(softly -> {
            softly.assertThat(finalBestSolution.getScore())
                    .isEqualTo(SimpleScore.ZERO);
            softly.assertThat(finalBestSolution.getEntityList().get(0).getValue())
                    .isEqualTo(solution.getValueList().get(0));
            softly.assertThat(finalBestSolution.getEntityList().get(1).getValue())
                    .isEqualTo(solution.getValueList().get(1));
        });
    }

    @Test
    void solveBasicVariableAllowedUnassigned() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAllowsUnassignedSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));
        var solver = (DefaultSolver<TestdataAllowsUnassignedSolution>) SolverFactory
                .<TestdataAllowsUnassignedSolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(1, 2);
        for (TestdataAllowsUnassignedEntity entity : solution.getEntityList()) { // Make sure nothing is set.
            entity.setValue(null);
        }

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {

            @Override
            public void stepStarted(AbstractStepScope<TestdataAllowsUnassignedSolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataAllowsUnassignedSolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 3) {
                        fail("The exhaustive search phase was not ended after 3 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);

        assertSoftly(softly -> {
            softly.assertThat(finalBestSolution.getScore())
                    .isEqualTo(SimpleScore.of(-1));
            softly.assertThat(finalBestSolution.getEntityList().get(0).getValue())
                    .as("The first entity's value was set.")
                    .isNull();
            softly.assertThat(finalBestSolution.getEntityList().get(1).getValue())
                    .as("The second entity's value was not set.")
                    .isNotNull();
        });
    }

    @Test
    void solveListVariableNotAllowedUnassigned() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestdataListVarEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));
        var solver = (DefaultSolver<TestdataListSolution>) SolverFactory.<TestdataListSolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataListSolution.generateUninitializedSolution(3, 2);

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void stepStarted(AbstractStepScope<TestdataListSolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataListSolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 25) {
                        fail("The exhaustive search phase was not ended after 25 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);
        var unassignedValues = finalBestSolution.getValueList().stream().filter(value -> value.getEntity() == null).toList();
        assertThat(unassignedValues).isEmpty();
    }

    @Test
    void solveListVariableAllowedUnassigned() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataAllowsUnassignedValuesListSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));
        var solver = (DefaultSolver<TestdataAllowsUnassignedValuesListSolution>) SolverFactory
                .<TestdataAllowsUnassignedValuesListSolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(3, 2);

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void stepStarted(AbstractStepScope<TestdataAllowsUnassignedValuesListSolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataAllowsUnassignedValuesListSolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 25) {
                        fail("The exhaustive search phase was not ended after 25 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);
        var unassignedValues = finalBestSolution.getValueList().stream().filter(value -> value.getEntity() == null).toList();
        assertThat(unassignedValues).hasSize(3);
    }

    @Test
    void solveWithMixedModel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMixedSolution.class)
                .withEntityClasses(TestdataMixedEntity.class, TestdataMixedOtherValue.class, TestdataMixedValue.class)
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));

        var solver = (DefaultSolver<TestdataMixedSolution>) SolverFactory.<TestdataMixedSolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataMixedSolution.generateUninitializedSolution(2, 3, 3);

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void stepStarted(AbstractStepScope<TestdataMixedSolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataMixedSolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 60) {
                        fail("The exhaustive search phase was not ended after 60 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);
        var unassignedValues = finalBestSolution.getValueList().stream().filter(value -> value.getEntity() == null).toList();
        assertThat(unassignedValues).isEmpty();
        var unassignedOtherValues = finalBestSolution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null).toList();
        assertThat(unassignedOtherValues).isEmpty();

    }

    @Test
    void solveWithMixedModelMultipleEntities() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMixedMultiEntitySolution.class)
                .withEntityClasses(TestdataMixedMultiEntityFirstEntity.class, TestdataMixedMultiEntitySecondEntity.class)
                .withEasyScoreCalculatorClass(TestdataMixedEntityEasyScoreCalculator.class)
                .withPhases(new ExhaustiveSearchPhaseConfig()
                        .withExhaustiveSearchType(ExhaustiveSearchType.BRUTE_FORCE));

        var solver = (DefaultSolver<TestdataMixedMultiEntitySolution>) SolverFactory
                .<TestdataMixedMultiEntitySolution> create(solverConfig)
                .buildSolver();

        var solution = TestdataMixedMultiEntitySolution.generateUninitializedSolution(2, 3, 3);

        solver.addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void stepStarted(AbstractStepScope<TestdataMixedMultiEntitySolution> stepScope) {
                if (stepScope instanceof ExhaustiveSearchStepScope<TestdataMixedMultiEntitySolution> exhaustiveSearchStepScope) {
                    if (exhaustiveSearchStepScope.getStepIndex() == 60) {
                        fail("The exhaustive search phase was not ended after 60 steps.");
                    }
                } else {
                    fail("Wrong phase was started: " + stepScope.getClass().getSimpleName());
                }
            }

        });

        var finalBestSolution = solver.solve(solution);
        var sizeAssignedValues = finalBestSolution.getEntityList().stream().mapToInt(e -> e.getValueList().size()).sum();
        assertThat(sizeAssignedValues).isEqualTo(solution.getValueList().size());
        var unassignedOtherValues = finalBestSolution.getOtherEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null).toList();
        assertThat(unassignedOtherValues).isEmpty();

    }

}
