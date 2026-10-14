package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolveResult;
import ai.timefold.solver.core.api.solver.SolverRunMetadata;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultSolveResult<Solution_>(Solution_ solution, SolverRunMetadata metadata)
        implements
            SolveResult<Solution_>,
            FinalBestSolutionEvent<Solution_> {

    @Override
    public Duration solvingDuration() {
        return metadata.solvingDuration();
    }

    @Override
    public long scoreCalculationCount() {
        return metadata.scoreCalculationCount();
    }

    @Override
    public long scoreCalculationSpeed() {
        return metadata.scoreCalculationSpeed();
    }

    @Override
    public long moveEvaluationCount() {
        return metadata.moveEvaluationCount();
    }

    @Override
    public long moveEvaluationSpeed() {
        return metadata.moveEvaluationSpeed();
    }

    @Override
    public boolean solutionInitialized() {
        return metadata.solutionInitialized();
    }

    @Override
    public ProblemSizeStatistics problemSizeStatistics() {
        return metadata.problemSizeStatistics();
    }

}
