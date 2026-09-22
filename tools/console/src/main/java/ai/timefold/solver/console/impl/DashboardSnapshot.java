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
        long entityCount,
        long variableCount,
        long approximateValueCount,
        String approximateProblemScaleText, // null until known, mirrors phaseName's null convention
        List<String> scoreLevelLabels, // one per entry of bestScoreHistoryByLevel; empty until known
        List<List<Double>> bestScoreHistoryByLevel,
        List<String> finishedPhaseLines) {

}
