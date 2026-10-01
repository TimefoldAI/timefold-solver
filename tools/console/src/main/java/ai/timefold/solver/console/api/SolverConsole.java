package ai.timefold.solver.console.api;

import java.nio.file.Path;

import ai.timefold.solver.console.impl.SolverDashboard;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.solver.DefaultSolver;

/**
 * Solves a planning problem while rendering a live terminal dashboard of its progress,
 * as a drop-in replacement for calling {@link SolverFactory#buildSolver()} and
 * {@link Solver#solve(Object)} directly.
 * <p>
 * If an interactive terminal is available, it redirects {@link System#out} and {@link System#err} to a log file
 * for the duration of the call.
 * A logging backend that caches its output stream at construction
 * (some Logback or Log4j2 console appenders)
 * is not covered;
 * lower {@code ai.timefold.solver}'s level in that config instead.
 * <p>
 * If an interactive terminal is not available,
 * the dashboard is skipped instead (a message is logged) and the problem is solved plainly,
 * with no output redirect.
 * <p>
 * Not safe to call concurrently from multiple threads in the same JVM.
 */
public final class SolverConsole {

    private static final String DEFAULT_LOG_FILE_NAME = "timefold-console.out";

    private SolverConsole() {
    }

    /**
     * As {@link #solve(SolverFactory, Object, Path)}, logging to {@value #DEFAULT_LOG_FILE_NAME} in the working directory.
     */
    public static <Solution_> Solution_ solve(SolverFactory<Solution_> solverFactory, Solution_ problem) {
        return solve(solverFactory, problem, Path.of(DEFAULT_LOG_FILE_NAME));
    }

    /**
     * Solves the problem exactly as {@link Solver#solve(Object)} would, while rendering a live dashboard to the terminal.
     *
     * @param solverFactory built from the caller's own domain and configuration
     * @param problem the unsolved problem
     * @param logFile where solver output is redirected for the duration of the call
     * @return the best solution found
     */
    public static <Solution_> Solution_ solve(SolverFactory<Solution_> solverFactory, Solution_ problem, Path logFile) {
        var solver = (DefaultSolver<Solution_>) solverFactory.buildSolver();
        if (SolverDashboard.isDisabledUnderQuarkusDevMode()) {
            return solver.solve(problem);
        }
        var dashboard = new SolverDashboard<>(logFile, solver, solver.getSolverScope());
        if (!dashboard.start()) {
            return solver.solve(problem);
        }
        solver.addPhaseLifecycleListener(dashboard);
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
