package ai.timefold.solver.core.impl.solver;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultSolverTerminationTest {

    @Test
    void stepCountTerminationAtSolverLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withStepCountLimit(1));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        // 2 entities means 2 steps, but the step count limit is 1.
        // Therefore the best solution is uninitialized.
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ofUninitialized(-1, 0));
    }

    @Test
    void stepCountTerminationAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withStepCountLimit(1)));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        // 2 entities means 2 steps, but the step count limit is 1.
        // Therefore the best solution is uninitialized.
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ofUninitialized(-1, 0));
    }

    @Test
    void diminishedReturnsTerminationAtSolverLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withTerminationConfig(new TerminationConfig()
                        .withDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig()));
        var solution = TestdataSolution.generateSolution(2, 2);
        solution.getEntityList().forEach(entity -> entity.setValue(null)); // Uninitialize.
        var solver = SolverFactory.<TestdataSolution> create(solverConfig)
                .buildSolver();
        var bestSolution = solver.solve(solution);
        Assertions.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void diminishedReturnsTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withDiminishedReturnsConfig(new DiminishedReturnsTerminationConfig())));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("DiminishedReturns");
    }

    @Test
    void unimprovedStepCountTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedStepCountLimit(1)));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("UnimprovedStepCount");
    }

    @Test
    void unimprovedTimeSpentTerminationInapplicableAtPhaseLevel() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withTerminationConfig(new TerminationConfig()
                                .withUnimprovedMillisecondsSpentLimit(1L)));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("includes some terminations which are not applicable")
                .hasMessageContaining("UnimprovedTimeMillisSpent");
    }

}
