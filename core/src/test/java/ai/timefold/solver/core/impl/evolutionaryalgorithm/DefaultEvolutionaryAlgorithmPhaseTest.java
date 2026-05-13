package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.evolutionaryalgorithm.EvolutionaryAlgorithmPhaseConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.util.MutableLong;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class DefaultEvolutionaryAlgorithmPhaseTest extends AbstractMeterTest {

    @Test
    void solveListVariable() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestingListSingleValueEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());

        var solution = TestdataListSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveBasicVariable() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestingSingleValueEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("-3"))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());

        var solution = TestdataSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveMultiBasicVariable() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.EVOLUTIONARY_ALGORITHM)
                .withSolutionClass(TestdataMultiVarSolution.class)
                .withEntityClasses(TestdataMultiVarEntity.class)
                .withEasyScoreCalculatorClass(TestingMultiVarEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("-6"))
                .withPhases(new EvolutionaryAlgorithmPhaseConfig());

        var solution = TestdataMultiVarSolution.generateUninitializedSolution(3, 3);
        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    public static final class TestingListSingleValueEasyScoreCalculator
            implements EasyScoreCalculator<TestdataListSolution, SimpleScore> {
        public @NonNull SimpleScore calculateScore(@NonNull TestdataListSolution solution) {
            var sum = new MutableLong(0);
            solution.getEntityList().forEach(e -> {
                int size = e.getValueList().size();
                if (size == 0) {
                    sum.increment();
                } else if (size > 1) {
                    double penalty = Math.pow(size - 1, 2);
                    sum.add((long) penalty);
                }
            });
            return SimpleScore.of(-sum.intValue());
        }
    }

    public static final class TestingSingleValueEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution solution) {
            var sum = new MutableLong(0);
            var set = new HashSet<TestdataValue>();
            solution.getEntityList().forEach(e -> {
                if (e.getValue() == null) {
                    sum.add(5L);
                    return;
                }
                if (set.contains(e.getValue())) {
                    sum.add(5L);
                }
                set.add(e.getValue());
            });
            sum.add(set.size());
            return SimpleScore.of(-sum.intValue());
        }
    }

    public static final class TestingMultiVarEasyScoreCalculator
            implements EasyScoreCalculator<TestdataMultiVarSolution, SimpleScore> {
        public @NonNull SimpleScore calculateScore(@NonNull TestdataMultiVarSolution solution) {
            var sum = new MutableLong(0);
            var primarySet = new HashSet<TestdataValue>();
            var secondarySet = new HashSet<TestdataValue>();
            solution.getMultiVarEntityList().forEach(e -> {
                if (e.getPrimaryValue() == null) {
                    sum.add(5L);
                } else {
                    if (primarySet.contains(e.getPrimaryValue())) {
                        sum.add(5L);
                    }
                    primarySet.add(e.getPrimaryValue());
                }
                if (e.getSecondaryValue() == null) {
                    sum.add(5L);
                } else {
                    if (secondarySet.contains(e.getSecondaryValue())) {
                        sum.add(5L);
                    }
                    secondarySet.add(e.getSecondaryValue());
                }
            });
            sum.add(primarySet.size() + secondarySet.size());
            return SimpleScore.of(-sum.intValue());
        }
    }

}
