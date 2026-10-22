package ai.timefold.solver.service.definition.internal.events;

import java.time.Duration;

import ai.timefold.solver.core.api.solver.ProblemSizeStatistics;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A copy of the {@link SolverRunSummary} values, detached from the object that provided them,
 * so that it does not keep a reference to the solution.
 */
@NullMarked
public record SolverRunSummaryDto(Duration solvingDuration, long scoreCalculationCount, long scoreCalculationSpeed,
        long moveEvaluationCount, long moveEvaluationSpeed, boolean solutionInitialized,
        ProblemSizeStatistics problemSizeStatistics, boolean terminatedEarly, @Nullable TerminationId termination,
        @Nullable PhaseId lastPhase, Duration timeToBestSolution)
        implements
            SolverRunSummary {

    public static SolverRunSummaryDto of(SolverRunSummary summary) {
        return new SolverRunSummaryDto(summary.solvingDuration(), summary.scoreCalculationCount(),
                summary.scoreCalculationSpeed(), summary.moveEvaluationCount(), summary.moveEvaluationSpeed(),
                summary.solutionInitialized(), summary.problemSizeStatistics(), summary.terminatedEarly(),
                summary.termination(), summary.lastPhase(), summary.timeToBestSolution());
    }

}
