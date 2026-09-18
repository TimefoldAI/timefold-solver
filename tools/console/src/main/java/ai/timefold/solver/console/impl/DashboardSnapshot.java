package ai.timefold.solver.console.impl;

import java.util.List;

/**
 * Immutable snapshot of solver progress at a single point in time, used to render one frame.
 * Decoupled from live solver scope objects so that {@link DashboardRenderer#render} can be a pure
 * function.
 */
record DashboardSnapshot(
        long elapsedMillis,
        String phaseName,
        long stepIndex,
        String bestScoreText,
        String stepScoreText,
        String acceptedPercentText,
        long moveEvaluationSpeed,
        long moveEvaluationCount,
        long scoreCalculationCount,
        long entityCount,
        List<Long> bestScoreHistory,
        List<String> finishedPhaseLines) {

}
