package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunMetadata;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.MathUtils;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultSolverRunMetadata(Duration solvingDuration, long scoreCalculationCount, long moveEvaluationCount,
        boolean solutionInitialized, ProblemSizeStatistics problemSizeStatistics)
        implements
            SolverRunMetadata {

    /**
     * Call only after the run ended.
     */
    public static SolverRunMetadata of(SolverScope<?> solverScope) {
        var runStatistics = solverScope.getRunStatistics();
        return new DefaultSolverRunMetadata(Duration.ofMillis(runStatistics.getTimeMillisSpent()),
                runStatistics.getScoreCalculationCount(), runStatistics.getMoveEvaluationCount(),
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
