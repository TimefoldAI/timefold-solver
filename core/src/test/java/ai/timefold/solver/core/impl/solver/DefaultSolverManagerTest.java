package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;

import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.TestdataListVarEasyScoreCalculator;

import org.junit.jupiter.api.Test;

class DefaultSolverManagerTest {

    @Test
    void onBestSolutionEventDoesNotChangeBestSolution() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestdataListVarEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new KOptListMoveSelectorConfig().withMinimumK(2).withMaximumK(3))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(10)));
        try (DefaultSolverManager<TestdataListSolution> solverManager =
                (DefaultSolverManager<TestdataListSolution>) SolverManager.<TestdataListSolution> create(solverConfig)) {
            var changesProcessed = new CountDownLatch(1);
            var solverJob =
                    solverManager.buildJob(1L, id -> TestdataListSolution.generateUninitializedSolution(5, 2), event -> {
                        // The first time we run the consumer, we clear the best solution created by CH.
                        // Therefore, no internal instances must be shared with the external consumers.
                        // Otherwise,
                        // clearing this would cause the LS to fail, as the model does not accept unassigned values.
                        event.solution().getEntityList().forEach(entity -> entity.getValueList().clear());
                        changesProcessed.countDown();
                    }, null, null, null, null, new SolverConfigOverride());
            // We must process the changes before allowing AbstractSolver to set the current working solution as the current best one
            solverJob.addSolverEventListener(event -> {
                try {
                    // Wait for the changes to be processed
                    changesProcessed.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            assertThatCode(() -> solverManager
                    .solve(solverJob)
                    .getFinalBestSolution())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void onSolverStartedEventDoesNotChangeBestSolution() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestdataListVarEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new KOptListMoveSelectorConfig().withMinimumK(2).withMaximumK(3))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(10)));
        try (DefaultSolverManager<TestdataListSolution> solverManager =
                (DefaultSolverManager<TestdataListSolution>) SolverManager.<TestdataListSolution> create(solverConfig)) {
            var changesProcessed = new CountDownLatch(1);
            var solverJob =
                    solverManager.buildJob(1L, id -> TestdataListSolution.generateInitializedSolution(5, 2), event -> {
                        // The solver start event call the best consumer if the solution is initialized.
                        // Therefore, no internal instances must be shared with the external consumers.
                        // Otherwise,
                        // clearing this would cause the LS to fail, as the model does not accept unassigned values.
                        event.solution().getEntityList().forEach(entity -> entity.getValueList().clear());
                        changesProcessed.countDown();
                    }, null, null, null, null, new SolverConfigOverride());
            // We must process the changes before allowing AbstractSolver to set the current working solution as the current best one
            solverJob.addPhaseEventListener(new PhaseLifecycleListener<>() {
                @Override
                public void solvingStarted(SolverScope<TestdataListSolution> solverScope) {
                    // Do nothing
                }

                @Override
                public void solvingEnded(SolverScope<TestdataListSolution> solverScope) {
                    // Do nothing
                }

                @Override
                public void phaseStarted(AbstractPhaseScope<TestdataListSolution> phaseScope) {
                    // Do nothing
                }

                @Override
                public void phaseEnded(AbstractPhaseScope<TestdataListSolution> phaseScope) {
                    try {
                        // We ensure the CH ends after the changes have been processed.
                        // Thus, the LS phase would start with a modified solution,
                        // and it would fail if they were shared.
                        if (phaseScope.getPhaseIndex() == 0) {
                            changesProcessed.await();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void stepStarted(AbstractStepScope<TestdataListSolution> stepScope) {
                    // Do nothing
                }

                @Override
                public void stepEnded(AbstractStepScope<TestdataListSolution> stepScope) {
                    // Do nothing
                }
            });
            assertThatCode(() -> solverManager
                    .solve(solverJob)
                    .getFinalBestSolution())
                    .doesNotThrowAnyException();
        }
    }
}
