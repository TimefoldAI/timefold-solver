package ai.timefold.solver.console.impl;

/**
 * Immutable summary of a completed solve,
 * printed once after the live dashboard closes.
 * Decoupled from live solver scope objects
 * so that {@link DashboardRenderer#renderFinalSummary} can be a pure function.
 */
record FinalSummary(
        String title,
        String finalScoreText,
        long steps,
        long solveTimeMillis,
        long movesEvaluated,
        long movesEvaluationSpeed,
        long movesAccepted,
        String acceptanceText,
        long scoreCalculationCount) {

}
