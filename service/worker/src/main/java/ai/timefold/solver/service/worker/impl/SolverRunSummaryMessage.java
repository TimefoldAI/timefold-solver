package ai.timefold.solver.service.worker.impl;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Locale;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.SolverRunSummary;
import ai.timefold.solver.core.api.solver.phase.PhaseId;
import ai.timefold.solver.core.api.solver.termination.TerminationId;
import ai.timefold.solver.core.api.solver.termination.TerminationType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builds the one-line log message that summarizes a finished solver run.
 */
@NullMarked
final class SolverRunSummaryMessage {

    static String of(String id, @Nullable Score<?> bestScore, SolverRunSummary runSummary) {
        var problemSizeStatistics = runSummary.problemSizeStatistics();
        return String.format(Locale.ROOT, "Solver run (%s) ended: outcome (%s), last phase (%s), time spent (%d ms), "
                + "time to best solution (%d ms, %d%% of run), best score (%s), solution initialized (%s), "
                + "move evaluations (%d, %d/sec), score calculations (%d, %d/sec), "
                + "problem size (entities %d, variables %d, values ~%d, search space ~10^%.1f).",
                id,
                describeOutcome(runSummary),
                describePhase(runSummary.lastPhase()),
                runSummary.solvingDuration().toMillis(),
                runSummary.timeToBestSolution().toMillis(),
                percentOf(runSummary.timeToBestSolution(), runSummary.solvingDuration()),
                bestScore,
                runSummary.solutionInitialized(),
                runSummary.moveEvaluationCount(), runSummary.moveEvaluationSpeed(),
                runSummary.scoreCalculationCount(), runSummary.scoreCalculationSpeed(),
                problemSizeStatistics.entityCount(),
                problemSizeStatistics.variableCount(),
                problemSizeStatistics.approximateValueCount(),
                problemSizeStatistics.approximateProblemSizeLog());
    }

    static String describeOutcome(SolverRunSummary runSummary) {
        var termination = runSummary.termination();
        if (termination != null) {
            return termination.type() + " at " + describeLocation(termination);
        }
        if (runSummary.terminatedEarly()) {
            return "terminated early";
        }
        // No termination ended the run; the phases ran out. See SolverRunSummary.termination().
        return runSummary.lastPhase() == null ? "no phase ran" : "phases finished without a termination";
    }

    /**
     * Writes the place of the termination in the termination configuration as a path,
     * such as {@code solver/termination/termination[1]} or {@code phase[1]/termination}.
     * A single termination is written as the path of the element that contains it;
     * an {@link TerminationType#AND} element as its own path.
     */
    static String describeLocation(TerminationId termination) {
        var path = new ArrayDeque<String>();
        var element = termination.type() == TerminationType.AND ? termination : termination.parent();
        for (var terminationId = element; terminationId != null; terminationId = terminationId.parent()) {
            path.addFirst(terminationId.parent() == null ? "termination" : "termination[" + terminationId.ordinal() + "]");
        }
        var phaseIndex = termination.phaseIndex();
        path.addFirst(phaseIndex.isPresent() ? "phase[" + phaseIndex.getAsInt() + "]" : "solver");
        return String.join("/", path);
    }

    static String describePhase(@Nullable PhaseId phase) {
        return phase == null ? "none" : phase.type().getPhaseName() + " phase[" + phase.phaseIndex() + "]";
    }

    static long percentOf(Duration part, Duration whole) {
        return whole.isZero() ? 0 : part.toMillis() * 100 / whole.toMillis();
    }

    private SolverRunSummaryMessage() {
    }

}
