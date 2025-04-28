package ai.timefold.solver.core.impl.phase.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

class DefaultCustomPhaseTest {

    private static final Duration RUN_TIME = Duration.ofMillis(10L);

    @Test
    void solverTermination() {
        var solverConfig = new SolverConfig()
                .withTerminationConfig(new TerminationConfig().withSpentLimit(RUN_TIME))
                .withPhases(
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(new LoopingPhaseCommand()))
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        var solution = TestdataSolution.generateSolution(2, 2);
        var duration = measure(() -> solver.solve(solution));
        assertThat(duration).isGreaterThanOrEqualTo(RUN_TIME);
    }

    @Test
    void phaseTermination() {
        var solverConfig = new SolverConfig()
                .withPhases(
                        new CustomPhaseConfig()
                                .withTerminationConfig(new TerminationConfig()
                                        .withSpentLimit(RUN_TIME))
                                .withCustomPhaseCommands(new LoopingPhaseCommand()))
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class);
        var solver = SolverFactory.create(solverConfig).buildSolver();
        var solution = TestdataSolution.generateSolution(2, 2);
        var duration = measure(() -> solver.solve(solution));
        assertThat(duration).isGreaterThanOrEqualTo(RUN_TIME);
    }

    private static Duration measure(Runnable runnable) {
        var milliTime = System.currentTimeMillis();
        runnable.run();
        return Duration.ofMillis(System.currentTimeMillis() - milliTime);
    }

    @NullMarked
    private static final class LoopingPhaseCommand implements PhaseCommand<TestdataSolution> {

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataSolution> scoreDirector, BooleanSupplier isPhaseTerminated) {
            while (true) {
                if (isPhaseTerminated.getAsBoolean()) { // Terminate when signal received.
                    return;
                }
            }
        }

    }

}
