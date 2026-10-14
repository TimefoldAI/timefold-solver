package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.MathUtils;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultSolverRunSummary(Duration solvingDuration, long scoreCalculationCount, long moveEvaluationCount,
        boolean isSolutionInitialized, ProblemSizeStatistics problemSizeStatistics)
        implements
            SolverRunSummary {

    /**
     * Call only after the run ended.
     */
    public static SolverRunSummary of(SolverScope<?> solverScope) {
        var solverStatistics = solverScope.getSolverStatistics();
        return new DefaultSolverRunSummary(Duration.ofMillis(solverStatistics.getTimeMillisSpent()),
                solverStatistics.getScoreCalculationCount(), solverStatistics.getMoveEvaluationCount(),
                solverScope.isBestSolutionInitialized(), solverScope.getProblemSizeStatistics());
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
