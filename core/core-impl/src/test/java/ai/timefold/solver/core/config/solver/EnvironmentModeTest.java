package ai.timefold.solver.core.config.solver;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.solver.testutil.calculator.TestdataCorruptedDifferentValuesCalculator;
import ai.timefold.solver.core.config.solver.testutil.calculator.TestdataDifferentValuesCalculator;
import ai.timefold.solver.core.config.solver.testutil.corruptedmove.factory.TestdataCorruptedEntityUndoMoveFactory;
import ai.timefold.solver.core.config.solver.testutil.corruptedmove.factory.TestdataCorruptedUndoMoveFactory;
import ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow.CorruptedUndoShadowEasyScoreCalculator;
import ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow.CorruptedUndoShadowEntity;
import ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow.CorruptedUndoShadowSolution;
import ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow.CorruptedUndoShadowValue;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveListFactory;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnvironmentModeTest {

    private static final int NUMBER_OF_RANDOM_NUMBERS_GENERATED = 1000;
    private static final int NUMBER_OF_TIMES_RUN = 10;
    private static final int NUMBER_OF_TERMINATION_STEP_COUNT_LIMIT = 20;

    private static TestdataSolution inputProblem;

    @BeforeAll
    static void setUpInputProblem() {
        inputProblem = new TestdataSolution("s1");
        inputProblem.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2"),
                new TestdataValue("v3")));
        inputProblem.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"),
                new TestdataEntity("e3"), new TestdataEntity("e4")));
    }

    private static SolverConfig buildSolverConfig(EnvironmentMode environmentMode) {
        CustomPhaseConfig initializerPhaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommandClassList(Collections.singletonList(TestdataFirstValueInitializer.class));

        LocalSearchPhaseConfig localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig
                .setTerminationConfig(
                        new TerminationConfig().withStepCountLimit(NUMBER_OF_TERMINATION_STEP_COUNT_LIMIT));

        return new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(environmentMode)
                .withPhases(initializerPhaseConfig, localSearchPhaseConfig);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EnvironmentMode.class)
    void determinism(EnvironmentMode environmentMode) {
        SolverConfig solverConfig = buildSolverConfig(environmentMode);
        setSolverConfigCalculatorClass(solverConfig, TestdataDifferentValuesCalculator.class);

        Solver<TestdataSolution> solver1 = SolverFactory.<TestdataSolution> create(solverConfig).buildSolver();
        Solver<TestdataSolution> solver2 = SolverFactory.<TestdataSolution> create(solverConfig).buildSolver();

        switch (environmentMode) {
            case NON_REPRODUCIBLE -> {
                assertNonReproducibility(solver1, solver2);
            }
            case TRACKED_FULL_ASSERT,
                    FULL_ASSERT,
                    FAST_ASSERT,
                    NON_INTRUSIVE_FULL_ASSERT,
                    REPRODUCIBLE -> {
                assertReproducibility(solver1, solver2);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EnvironmentMode.class)
    void corruptedCustomMoves(EnvironmentMode environmentMode) {
        SolverConfig solverConfig = buildSolverConfig(environmentMode);
        // Intrusive modes should throw exception about corrupted undoMove
        setSolverConfigCalculatorClass(solverConfig, TestdataDifferentValuesCalculator.class);

        switch (environmentMode) {
            case TRACKED_FULL_ASSERT -> {
                setSolverConfigMoveListFactoryClassToCorrupted(
                        solverConfig,
                        TestdataCorruptedUndoMoveFactory.class);
                assertIllegalStateExceptionWhileSolving(solverConfig, "corrupted undoMove",
                        "Variables that are different between before and undo",
                        "Actual value (v2) of variable value on TestdataEntity entity (e2) differs from expected (v1)");
            }
            case FULL_ASSERT,
                    FAST_ASSERT -> {
                setSolverConfigMoveListFactoryClassToCorrupted(
                        solverConfig,
                        TestdataCorruptedUndoMoveFactory.class);
                assertIllegalStateExceptionWhileSolving(solverConfig, "corrupted undoMove");
            }
            case NON_INTRUSIVE_FULL_ASSERT -> {
                setSolverConfigMoveListFactoryClassToCorrupted(
                        solverConfig,
                        TestdataCorruptedEntityUndoMoveFactory.class);
                assertIllegalStateExceptionWhileSolving(solverConfig, "not the uncorruptedScore");
            }
            case REPRODUCIBLE,
                    NON_REPRODUCIBLE -> {
                // No exception expected
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EnvironmentMode.class)
    void corruptedUndoShadowVariableListener(EnvironmentMode environmentMode) {
        SolverConfig solverConfig = new SolverConfig()
                .withEnvironmentMode(environmentMode)
                .withSolutionClass(CorruptedUndoShadowSolution.class)
                .withEntityClasses(CorruptedUndoShadowEntity.class, CorruptedUndoShadowValue.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withEasyScoreCalculatorClass(CorruptedUndoShadowEasyScoreCalculator.class))
                .withTerminationConfig(new TerminationConfig()
                        .withScoreCalculationCountLimit(10L));

        switch (environmentMode) {
            case TRACKED_FULL_ASSERT -> {
                var e1 = new CorruptedUndoShadowEntity("e1");
                var e2 = new CorruptedUndoShadowEntity("e2");
                var v1 = new CorruptedUndoShadowValue("v1");
                var v2 = new CorruptedUndoShadowValue("v2");

                e1.setValue(v1);
                e1.setValueClone(v1);
                v1.setEntities(new ArrayList<>(List.of(e1)));

                e2.setValue(v2);
                e2.setValueClone(v2);
                v2.setEntities(new ArrayList<>(List.of(e2)));
                assertThatExceptionOfType(IllegalStateException.class)
                        .isThrownBy(() -> PlannerTestUtils.solve(solverConfig,
                                new CorruptedUndoShadowSolution(List.of(e1, e2),
                                        List.of(v1, v2))))
                        .withMessageContainingAll("corrupted undoMove",
                                "Variables that are different between before and undo",
                                "Actual value (v2) of variable valueClone on CorruptedUndoShadowEntity entity (CorruptedUndoShadowEntity) differs from expected (v1)");
            }
            case FULL_ASSERT,
                    FAST_ASSERT -> {
                // FAST_ASSERT does not create snapshots since it is not intrusive, and hence it can only
                // detect the undo corruption and not what caused it
                var e1 = new CorruptedUndoShadowEntity("e1");
                var e2 = new CorruptedUndoShadowEntity("e2");
                var v1 = new CorruptedUndoShadowValue("v1");
                var v2 = new CorruptedUndoShadowValue("v2");

                e1.setValue(v1);
                e1.setValueClone(v1);
                v1.setEntities(new ArrayList<>(List.of(e1)));

                e2.setValue(v2);
                e2.setValueClone(v2);
                v2.setEntities(new ArrayList<>(List.of(e2)));
                assertThatExceptionOfType(IllegalStateException.class)
                        .isThrownBy(() -> PlannerTestUtils.solve(solverConfig,
                                new CorruptedUndoShadowSolution(List.of(e1, e2),
                                        List.of(v1, v2))))
                        .withMessageContainingAll("corrupted undoMove");
            }
            case REPRODUCIBLE,
                    NON_REPRODUCIBLE,
                    NON_INTRUSIVE_FULL_ASSERT -> {
                // No exception expected
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(EnvironmentMode.class)
    void corruptedConstraints(EnvironmentMode environmentMode) {
        SolverConfig solverConfig = buildSolverConfig(environmentMode);
        // For full assert modes it should throw exception about corrupted score
        setSolverConfigCalculatorClass(solverConfig, TestdataCorruptedDifferentValuesCalculator.class);

        switch (environmentMode) {
            case TRACKED_FULL_ASSERT -> {
                assertIllegalStateExceptionWhileSolving(
                        solverConfig,
                        "not the uncorruptedScore");
            }
            case FULL_ASSERT,
                    NON_INTRUSIVE_FULL_ASSERT -> {
                assertIllegalStateExceptionWhileSolving(
                        solverConfig,
                        "not the uncorruptedScore");
            }
            case FAST_ASSERT -> {
                assertIllegalStateExceptionWhileSolving(
                        solverConfig,
                        "Score corruption analysis could not be generated ");
            }
            case REPRODUCIBLE,
                    NON_REPRODUCIBLE -> {
                // No exception expected
            }
        }
    }

    private void assertReproducibility(Solver<TestdataSolution> solver1, Solver<TestdataSolution> solver2) {
        assertGeneratingSameNumbers(((DefaultSolver<TestdataSolution>) solver1).getRandomFactory(),
                ((DefaultSolver<TestdataSolution>) solver2).getRandomFactory());
        assertSameScoreSeries(solver1, solver2);
    }

    private void assertNonReproducibility(Solver<TestdataSolution> solver1, Solver<TestdataSolution> solver2) {
        assertGeneratingDifferentNumbers(((DefaultSolver<TestdataSolution>) solver1).getRandomFactory(),
                ((DefaultSolver<TestdataSolution>) solver2).getRandomFactory());
        assertDifferentScoreSeries(solver1, solver2);
    }

    private void assertIllegalStateExceptionWhileSolving(SolverConfig solverConfig, String... exceptionMessage) {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> PlannerTestUtils.solve(solverConfig, inputProblem))
                .withMessageContainingAll(exceptionMessage);
    }

    private void assertSameScoreSeries(Solver<TestdataSolution> solver1, Solver<TestdataSolution> solver2) {
        TestdataStepScoreListener listener = new TestdataStepScoreListener();
        TestdataStepScoreListener listener2 = new TestdataStepScoreListener();

        ((DefaultSolver<TestdataSolution>) solver1).addPhaseLifecycleListener(listener);
        ((DefaultSolver<TestdataSolution>) solver2).addPhaseLifecycleListener(listener2);

        assertSoftly(softly -> IntStream.range(0, NUMBER_OF_TIMES_RUN)
                .forEach(i -> {
                    solver1.solve(inputProblem);
                    solver2.solve(inputProblem);
                    softly.assertThat(listener.getScores())
                            .as("Score steps should be the same "
                                    + "in a reproducible environment mode.")
                            .isEqualTo(listener2.getScores());
                }));
    }

    private void assertDifferentScoreSeries(Solver<TestdataSolution> solver1, Solver<TestdataSolution> solver2) {
        TestdataStepScoreListener listener = new TestdataStepScoreListener();
        TestdataStepScoreListener listener2 = new TestdataStepScoreListener();

        ((DefaultSolver<TestdataSolution>) solver1).addPhaseLifecycleListener(listener);
        ((DefaultSolver<TestdataSolution>) solver2).addPhaseLifecycleListener(listener2);

        assertSoftly(softly -> IntStream.range(0, NUMBER_OF_TIMES_RUN)
                .forEach(i -> {
                    solver1.solve(inputProblem);
                    solver2.solve(inputProblem);
                    softly.assertThat(listener.getScores())
                            .as("Score steps should not be the same in a non-reproducible environment mode. "
                                    + "This might be possible because searchSpace is not infinite and "
                                    + "two different random scenarios can have the same results. "
                                    + "Run test again.")
                            .isNotEqualTo(listener2.getScores());
                }));
    }

    private void assertGeneratingSameNumbers(RandomFactory factory1, RandomFactory factory2) {
        Random random = factory1.createRandom();
        Random random2 = factory2.createRandom();

        assertSoftly(softly -> IntStream.range(0, NUMBER_OF_RANDOM_NUMBERS_GENERATED)
                .forEach(i -> softly.assertThat(random.nextInt())
                        .as("Random factories should generate the same results "
                                + "in a reproducible environment mode.")
                        .isEqualTo(random2.nextInt())));
    }

    private void assertGeneratingDifferentNumbers(RandomFactory factory1, RandomFactory factory2) {
        Random random = factory1.createRandom();
        Random random2 = factory2.createRandom();

        assertSoftly(softly -> IntStream.range(0, NUMBER_OF_RANDOM_NUMBERS_GENERATED)
                .forEach(i -> softly.assertThat(random.nextInt())
                        .as("Random factories should not generate exactly the same results "
                                + "in the non-reproducible environment mode. "
                                + "It can happen but the probability is very low. Run test again")
                        .isNotEqualTo(random2.nextInt())));
    }

    private void setSolverConfigCalculatorClass(SolverConfig solverConfig,
            Class<? extends EasyScoreCalculator> easyScoreCalculatorClass) {
        solverConfig.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(easyScoreCalculatorClass));
    }

    private void setSolverConfigMoveListFactoryClassToCorrupted(SolverConfig solverConfig,
            Class<? extends MoveListFactory<TestdataSolution>> move) {
        MoveListFactoryConfig moveListFactoryConfig = new MoveListFactoryConfig();
        moveListFactoryConfig.setMoveListFactoryClass(move);

        CustomPhaseConfig initializerPhaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommandClassList(Collections.singletonList(TestdataFirstValueInitializer.class));

        LocalSearchPhaseConfig localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig.setMoveSelectorConfig(moveListFactoryConfig);
        localSearchPhaseConfig
                .setTerminationConfig(
                        new TerminationConfig().withStepCountLimit(NUMBER_OF_TERMINATION_STEP_COUNT_LIMIT));

        solverConfig.withPhases(initializerPhaseConfig, localSearchPhaseConfig);
    }

    public static class TestdataFirstValueInitializer implements CustomPhaseCommand<TestdataSolution> {

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataSolution> scoreDirector) {
            TestdataSolution solution = scoreDirector.getWorkingSolution();
            TestdataValue firstValue = solution.getValueList().get(0);

            for (TestdataEntity entity : solution.getEntityList()) {
                scoreDirector.beforeVariableChanged(entity, "value");
                entity.setValue(firstValue);
                scoreDirector.afterVariableChanged(entity, "value");
            }

            scoreDirector.triggerVariableListeners();
            InnerScoreDirector<TestdataSolution, ?> innerScoreDirector =
                    (InnerScoreDirector<TestdataSolution, ?>) scoreDirector;
            Score<?> score = innerScoreDirector.calculateScore();

            if (!score.isSolutionInitialized()) {
                throw new IllegalStateException("The solution (" + TestdataEntity.class.getSimpleName()
                        + ") was not fully initialized by CustomSolverPhase: ("
                        + this.getClass().getCanonicalName() + ")");
            }
        }
    }

    public static class TestdataStepScoreListener extends PhaseLifecycleListenerAdapter<TestdataSolution> {

        private List<SimpleScore> scores = new ArrayList<>();

        @Override
        public void stepEnded(AbstractStepScope<TestdataSolution> stepScope) {
            TestdataSolution solution = stepScope.getWorkingSolution();

            if (solution.getScore() != null) {
                scores.add(solution.getScore());
            }
        }

        public List<SimpleScore> getScores() {
            return scores;
        }
    }
}
