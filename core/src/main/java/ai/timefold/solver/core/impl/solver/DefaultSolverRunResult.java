package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunResult;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultSolverRunResult<Solution_>(Solution_ solution, SolverRunSummary summary)
        implements
            SolverRunResult<Solution_>,
            FinalBestSolutionEvent<Solution_> {

    @Override
    public Duration solvingDuration() {
        return summary.solvingDuration();
    }

    @Override
    public long scoreCalculationCount() {
        return summary.scoreCalculationCount();
    }

    @Override
    public long scoreCalculationSpeed() {
        return summary.scoreCalculationSpeed();
    }

    @Override
    public long moveEvaluationCount() {
        return summary.moveEvaluationCount();
    }

    @Override
    public long moveEvaluationSpeed() {
        return summary.moveEvaluationSpeed();
    }

    @Override
    public boolean isSolutionInitialized() {
        return summary.isSolutionInitialized();
    }

    @Override
    public ProblemSizeStatistics problemSizeStatistics() {
        return summary.problemSizeStatistics();
    }

}
