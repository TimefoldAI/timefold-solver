package ai.timefold.solver.console.api;

import java.nio.file.Path;

import ai.timefold.solver.console.impl.SolverDashboard;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

/**
 * Solves a planning problem while rendering a live terminal dashboard of its progress, as a
 * drop-in replacement for calling {@link SolverFactory#buildSolver()} and
 * {@link ai.timefold.solver.core.api.solver.Solver#solve(Object)} directly.
 * <p>
 * While solving, this redirects {@link System#out} and {@link System#err} to a log file and
 * silences the {@code ai.timefold.solver} {@link java.util.logging.Logger}, for the duration of
 * the call, restoring both afterward. A plain (non-Quarkus) application whose own logging backend
 * caches its output stream at construction (for example some Logback or Log4j2 console appender
 * configurations) is not covered by either mechanism; lowering {@code ai.timefold.solver}'s level
 * in that application's own logging configuration is the fallback.
 * <p>
 * Requires a real interactive terminal; fails fast otherwise, including under {@code quarkus:dev}
 * (whose own raw-mode stdin hotkey handler would otherwise conflict with the dashboard's).
 * <p>
 * Under {@link ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig
 * partitioned search}, only the top-level solver is observed: the construction heuristic and local
 * search work happens in separate child solvers this dashboard has no access to, so step statistics
 * stay coarse (one update per partition, rather than per step) and the accepted move percentage
 * never populates.
 * <p>
 * Not safe to call concurrently from multiple threads in the same JVM.
 */
public final class SolverConsole {

    private static final String DEFAULT_LOG_FILE_NAME = "timefold-console.log";

    private SolverConsole() {
    }

    /**
     * As defined by {@link #solve(SolverFactory, Object, Path)},
     * logging to {@value #DEFAULT_LOG_FILE_NAME} in the current working directory.
     */
    public static <Solution_> Solution_ solve(SolverFactory<Solution_> solverFactory, Solution_ problem) {
        return solve(solverFactory, problem, Path.of(DEFAULT_LOG_FILE_NAME));
    }

    /**
     * Solves the problem exactly as {@link ai.timefold.solver.core.api.solver.Solver#solve(Object)}
     * would, while rendering a live dashboard of its progress to the terminal.
     *
     * @param solverFactory built from the caller's own domain and configuration, whether directly
     *        or injected by the Timefold Quarkus extension
     * @param problem the unsolved problem, as normally passed to
     *        {@link ai.timefold.solver.core.api.solver.Solver#solve(Object)}
     * @param logFile where solver output is redirected for the duration of the call
     * @return the best solution found, as
     *         {@link ai.timefold.solver.core.api.solver.Solver#solve(Object)} would return
     */
    @SuppressWarnings("unchecked")
    public static <Solution_> Solution_ solve(SolverFactory<Solution_> solverFactory, Solution_ problem, Path logFile) {
        // ponytail: cast to the sole known Solver implementation, since that's the only way to reach
        // step/phase-level data (impl-only, no compatibility guarantee). Fallback if this ever
        // breaks: SolverManager + SolverJob + withBestSolutionEventConsumer, minus step-level data.
        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        var dashboard = new SolverDashboard<Solution_>(logFile, solver, solver.getSolverScope());
        solver.addPhaseLifecycleListener(dashboard);
        dashboard.start();
        var solved = false;
        try {
            var solution = solver.solve(problem);
            solved = true;
            return solution;
        } finally {
            dashboard.stop(solved);
        }
    }

}
