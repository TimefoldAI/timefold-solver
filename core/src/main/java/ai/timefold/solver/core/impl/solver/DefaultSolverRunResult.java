package ai.timefold.solver.core.impl.solver;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunResult;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.event.FinalBestSolutionEvent;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    public boolean solutionInitialized() {
        return summary.solutionInitialized();
    }

    @Override
    public ProblemSizeStatistics problemSizeStatistics() {
        return summary.problemSizeStatistics();
    }

    @Override
    public boolean terminatedEarly() {
        return summary.terminatedEarly();
    }

    @Override
    public @Nullable TerminationId termination() {
        return summary.termination();
    }

    @Override
    public @Nullable PhaseId lastPhase() {
        return summary.lastPhase();
    }

    @Override
    public Duration timeToBestSolution() {
        return summary.timeToBestSolution();
    }

}
