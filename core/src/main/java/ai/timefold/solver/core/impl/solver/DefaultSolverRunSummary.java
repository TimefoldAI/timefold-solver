package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.MathUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record DefaultSolverRunSummary(Duration solvingDuration, long scoreCalculationCount, long moveEvaluationCount,
        boolean solutionInitialized, ProblemSizeStatistics problemSizeStatistics, boolean terminatedEarly,
        @Nullable TerminationId termination, @Nullable PhaseId lastPhase, Duration timeToBestSolution)
        implements
            SolverRunSummary {

    /**
     * Call only after the run ended.
     */
    public static SolverRunSummary of(SolverScope<?> solverScope, boolean terminatedEarly) {
        var solverStatistics = solverScope.getSolverStatistics();
        return new DefaultSolverRunSummary(Duration.ofMillis(solverStatistics.getTimeMillisSpent()),
                solverStatistics.getScoreCalculationCount(), solverStatistics.getMoveEvaluationCount(),
                solverScope.isBestSolutionInitialized(), solverScope.getProblemSizeStatistics(), terminatedEarly,
                // terminateEarly() can arrive while a configured termination is also true; the early end wins.
                terminatedEarly ? null : solverScope.getLastTerminationId(), solverScope.getLastPhase(),
                Duration.ofMillis(solverStatistics.getTimeToBestSolutionMillis()));
    }

    @Override
    public long scoreCalculationSpeed() {
        return MathUtils.getSpeed(scoreCalculationCount, solvingDuration.toMillis());
    }

    @Override
    public long moveEvaluationSpeed() {
        return MathUtils.getSpeed(moveEvaluationCount, solvingDuration.toMillis());
    }

}
