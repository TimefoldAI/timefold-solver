package ai.timefold.solver.console.impl;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Renders a live terminal dashboard while a solver runs, driven by phase/step lifecycle events.
 * Owns the terminal and the {@code System.out}/{@code System.err} redirection.
 *
 * @param <Solution_> the solution type
 */
public final class SolverDashboard<Solution_> extends PhaseLifecycleListenerAdapter<Solution_> {

    // One sample per repaint, so ~50s of full resolution before halving (not dropping) back to this limit;
    // storage transiently ranges up to 2x this between halvings.
    private static final int SPARKLINE_HISTORY_LIMIT = 200;
    private static final int PHASE_HISTORY_LIMIT = 50;
    private static final long REPAINT_PERIOD_MILLIS = 250L;
    private static final String CLEAR_SCREEN = "\033[H\033[2J"; // move cursor home, clear screen
    private static final String IDENTIFICATION = TimefoldSolverEnterpriseService.identifySolverVersion();
    private static final Logger LOGGER = LoggerFactory.getLogger(SolverDashboard.class);
    // Generous margin over how long real terminal detection ever takes (milliseconds),
    // so a hang in the platform's own terminal probing (observed on Windows)
    // degrades the same way a dumb terminal already does.
    private static final long TERMINAL_PROBE_TIMEOUT_SECONDS = 5L;

    private final Path logFile;
    private final Solver<Solution_> solver;
    private final SolverScope<Solution_> solverScope;

    private volatile String phaseName;
    private volatile long stepIndex = -1L;
    private volatile String stepScoreText = "n/a";
    private volatile String acceptedPercentText = "—";

    // Populated lazily on the first non-null best score (level count is unknown before that). Touched only
    // from paintFrame(), always under this dashboard's monitor, so no separate lock is needed.
    private List<String> scoreLevelLabels;
    private final List<ArrayDeque<Double>> bestScoreHistoryPerLevel = new ArrayList<>();
    private final List<String> finishedPhaseLines = Collections.synchronizedList(new ArrayList<>());

    // Written and read only from the solving thread; no volatile/synchronization needed, unlike the fields above.
    private long totalSteps;
    private long totalAcceptedMoveCount;
    private long totalSelectedMoveCount;

    private Terminal terminal;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private Thread repaintThread;
    private Thread keyThread;
    private volatile boolean running;
    private volatile boolean quitRequested;

    public SolverDashboard(Path logFile, Solver<Solution_> solver, SolverScope<Solution_> solverScope) {
        this.logFile = logFile;
        this.solver = solver;
        this.solverScope = solverScope;
    }

    // ************************************************************************
    // Lifecycle, called by SolverConsole
    // ************************************************************************

    /**
     * @return false if no interactive terminal is available (piped, redirected, or non-interactive output); logs
     *         why and leaves this dashboard entirely unstarted (no output redirect, no threads) so the caller can
     *         solve plainly instead
     */
    public boolean start() {
        terminal = tryBuildTerminal();
        if (terminal == null || terminal.getType().startsWith(Terminal.TYPE_DUMB)) {
            closeTerminalQuietly();
            terminal = null;
            // Necessary sysout - logging may be redirected to a file, leading to blank console.
            System.out.println(
                    "SolverConsole dashboard disabled: no interactive terminal available (piped, redirected, or non-interactive output).");
            return false;
        }
        try {
            redirectOutput();
            running = true;
            try {
                terminal.enterRawMode();
            } catch (UnsupportedOperationException ignored) {
                // Best effort; the 'q' quit key then only works after pressing Enter too.
            }
            terminal.handle(Terminal.Signal.WINCH, signal -> repaint());
            repaintThread = new Thread(this::repaintLoop, "solver-console-repaint");
            repaintThread.setDaemon(true);
            repaintThread.start();
            keyThread = new Thread(this::keyLoop, "solver-console-key");
            keyThread.setDaemon(true);
            keyThread.start();
            return true;
        } catch (RuntimeException | Error e) {
            // Nothing acquired above may outlive a failed start(), or it leaks for the rest of the JVM's life.
            running = false;
            restoreOutput();
            closeTerminalQuietly();
            throw e;
        }
    }

    /**
     * @param solved false if {@code solver.solve()} threw rather than returning;
     *        skips the final summary box, since the solver scope it would report on may be left inconsistent
     */
    public void stop(boolean solved) {
        int terminalWidth;
        // Also entered by repaint(): serializes terminal writes so none is in flight once this closes the terminal.
        synchronized (this) {
            running = false;
            repaintThread.interrupt();
            keyThread.interrupt();
            terminalWidth = terminal.getSize().getColumns();
            if (quitRequested) {
                // Otherwise the dashboard box would linger above the summary printed below.
                var writer = terminal.writer();
                writer.print(CLEAR_SCREEN);
                writer.flush();
            } else {
                paintFrame();
            }
            restoreOutput();
            closeTerminalQuietly();
        }
        // Printed after output is restored and the terminal is closed, so it reaches the real console.
        System.out.println();
        if (solved) {
            var summary = buildFinalSummary();
            for (var line : DashboardRenderer.renderFinalSummary(summary, terminalWidth)) {
                System.out.println(line);
            }
        } else {
            System.out.println("Solving did not complete; see the log for details.");
        }
        System.out.println();
    }

    private String finalScoreText() {
        var bestScore = solverScope.getBestScore();
        return bestScore == null ? "n/a" : bestScore.raw().toString();
    }

    FinalSummary buildFinalSummary() {
        var bestScore = solverScope.getBestScore();
        var title = bestScore == null ? "NO SOLUTION FOUND"
                : bestScore.isFullyAssigned() && bestScore.raw().isFeasible() ? "FEASIBLE SOLUTION FOUND"
                        : "INFEASIBLE SOLUTION FOUND";
        var acceptanceText = totalSelectedMoveCount == 0L ? "n/a"
                : String.format("%.1f %%", 100.0 * totalAcceptedMoveCount / totalSelectedMoveCount);
        return new FinalSummary(title, finalScoreText(), totalSteps, solverScope.getTimeMillisSpent(),
                solverScope.getMoveEvaluationCount(), solverScope.getMoveEvaluationSpeed(), totalAcceptedMoveCount,
                acceptanceText, solverScope.getScoreCalculationCount());
    }

    // ************************************************************************
    // PhaseLifecycleListener
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseName = phaseScope.getPhaseId().simpleProducerName();
        stepIndex = -1L;
        stepScoreText = "n/a";
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        stepIndex = stepScope.getStepIndex();
        // Unset for exhaustive search (score lives on the expanding node) and custom phases; fall back below.
        InnerScore<?> stepScore = stepScope.getScore();
        if (stepScore == null && stepScope instanceof ExhaustiveSearchStepScope<Solution_> exhaustiveStepScope) {
            stepScore = exhaustiveStepScope.getStartingStepScore();
        }
        stepScoreText = stepScore == null ? "n/a" : stepScore.raw().toString();
        if (stepScope instanceof LocalSearchStepScope<Solution_> localSearchStepScope) {
            var accepted = localSearchStepScope.getAcceptedMoveCount();
            var selected = localSearchStepScope.getSelectedMoveCount();
            if (accepted != null && selected != null) {
                // Construction heuristic steps have selected but no accepted count; only local search has both.
                totalAcceptedMoveCount += accepted;
                totalSelectedMoveCount += selected;
            }
        }
        // Cumulative across the whole run; step-based swings wildly step to step, the running total doesn't.
        acceptedPercentText = totalSelectedMoveCount == 0L ? "—"
                : String.format("%.1f %%", 100.0 * totalAcceptedMoveCount / totalSelectedMoveCount);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        totalSteps += phaseScope.getNextStepIndex(); // Steps are re-indexed from 0 per phase.
        var phaseTimeMillisSpent = phaseScope.calculatePhaseTimeMillisSpentUpToNow();
        var phaseMoveEvaluationSpeed =
                phaseScope.getPhaseMoveEvaluationCount() * 1000L / (phaseTimeMillisSpent == 0L ? 1L : phaseTimeMillisSpent);
        var line = String.format("%s  %s ended: %d steps, %,d/s, score %s",
                DashboardRenderer.formatElapsed(phaseScope.calculateSolverTimeMillisSpentUpToNow()),
                phaseScope.getPhaseId().simpleProducerName(), phaseScope.getNextStepIndex(), phaseMoveEvaluationSpeed,
                phaseScope.getBestScore().raw());
        synchronized (finishedPhaseLines) {
            finishedPhaseLines.add(line);
            while (finishedPhaseLines.size() > PHASE_HISTORY_LIMIT) {
                finishedPhaseLines.removeFirst();
            }
        }
    }

    // ************************************************************************
    // Rendering
    // ************************************************************************

    private void repaintLoop() {
        while (running) {
            repaint();
            try {
                Thread.sleep(REPAINT_PERIOD_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void keyLoop() {
        try {
            while (running) {
                var key = terminal.reader().read(REPAINT_PERIOD_MILLIS);
                if (key == 'q' || key == 'Q') {
                    quitRequested = true;
                    solver.terminateEarly();
                    return;
                }
            }
        } catch (IOException ignored) {
            // Terminal closed; nothing more to read.
        }
    }

    // Synchronized so no writer (repaint thread, WINCH handler, stop()'s final frame) touches the terminal
    // concurrently or after stop() closes it.
    private synchronized void repaint() {
        if (!running) {
            return;
        }
        paintFrame();
    }

    private void paintFrame() {
        recordBestScoreSample();
        var snapshot = buildSnapshot();
        var size = terminal.getSize();
        var lines = DashboardRenderer.render(snapshot, size.getColumns(), size.getRows(), IDENTIFICATION);
        var writer = terminal.writer();
        writer.print(CLEAR_SCREEN);
        for (var line : lines) {
            writer.println(line);
        }
        writer.flush();
    }

    /**
     * Samples once per repaint (fixed wall-clock cadence), not once per step,
     * so the sparkline's length reflects elapsed time rather than step count,
     * and unconditionally so a genuine plateau still shows as flat.
     */
    void recordBestScoreSample() {
        var bestScore = solverScope.getBestScore();
        if (bestScore == null) {
            return; // Solving hasn't started yet.
        }
        var levels = bestScore.raw().toLevelDoubles();
        if (scoreLevelLabels == null) {
            // Level count is unknown before solving starts, so populate here on first sample, not in the constructor.
            scoreLevelLabels = buildScoreLevelLabels(levels.length);
            for (var i = 0; i < levels.length; i++) {
                bestScoreHistoryPerLevel.add(new ArrayDeque<>());
            }
        }
        for (var i = 0; i < levels.length; i++) {
            var history = bestScoreHistoryPerLevel.get(i);
            history.addLast(levels[i]);
            if (history.size() >= 2 * SPARKLINE_HISTORY_LIMIT) {
                // Halve only at exactly double the limit, so every bucket merges 2:1 with no rounding remainder
                // (downsampling by 1 each step instead would stick the newest bucket at a stale average).
                var compacted = DashboardRenderer.downsample(List.copyOf(history), SPARKLINE_HISTORY_LIMIT);
                history.clear();
                history.addAll(compacted);
            }
        }
    }

    /**
     * One label per score level (e.g. {@code hard}, {@code medium}, {@code soft}), indexed like
     * {@link #bestScoreHistoryPerLevel}.
     */
    private List<String> buildScoreLevelLabels(int levelCount) {
        var rawLabels = solverScope.getScoreDefinition().getLevelLabels();
        var labels = new ArrayList<String>(levelCount);
        for (var rawLabel : rawLabels) {
            labels.add(rawLabel.endsWith(" score") ? rawLabel.substring(0, rawLabel.length() - " score".length()) : rawLabel);
        }
        return List.copyOf(labels);
    }

    DashboardSnapshot buildSnapshot() {
        var levelLabelsSnapshot = scoreLevelLabels == null ? List.<String> of() : scoreLevelLabels;
        var historySnapshot = new ArrayList<List<Double>>(bestScoreHistoryPerLevel.size());
        for (var history : bestScoreHistoryPerLevel) {
            historySnapshot.add(List.copyOf(history));
        }
        List<String> phaseLinesSnapshot;
        synchronized (finishedPhaseLines) {
            phaseLinesSnapshot = List.copyOf(finishedPhaseLines);
        }
        var startingMillis = solverScope.getStartingSystemTimeMillis();
        var elapsedMillis = startingMillis == null ? 0L : solverScope.calculateTimeMillisSpentUpToNow();
        var bestScore = solverScope.getBestScore();
        var problemSizeStatistics = solverScope.getProblemSizeStatistics();
        return new DashboardSnapshot(elapsedMillis, phaseName, stepIndex,
                bestScore == null ? "n/a" : bestScore.raw().toString(), stepScoreText, acceptedPercentText,
                solverScope.getMoveEvaluationSpeed(), solverScope.getMoveEvaluationCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.entityCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.variableCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.approximateValueCount(),
                problemSizeStatistics == null ? null : problemSizeStatistics.approximateProblemScaleAsFormattedString(),
                levelLabelsSnapshot, List.copyOf(historySnapshot), phaseLinesSnapshot);
    }

    // ************************************************************************
    // Environment guards
    // ************************************************************************

    /**
     * @return true under {@code quarkus:dev}, whose own raw-mode stdin hotkey handler would otherwise conflict
     *         with the dashboard's; logs a message explaining why the dashboard is skipped.
     */
    public static boolean isDisabledUnderQuarkusDevMode() {
        try {
            var launchModeClass = Class.forName("io.quarkus.runtime.LaunchMode");
            var current = launchModeClass.getMethod("current").invoke(null);
            if (!"DEVELOPMENT".equals(current.toString())) {
                return false;
            }
        } catch (ClassNotFoundException e) {
            return false; // Not a Quarkus application.
        } catch (ReflectiveOperationException e) {
            return false; // Quarkus is present but its LaunchMode API changed shape; don't block on a best-effort check.
        }
        LOGGER.warn("SolverConsole dashboard disabled under `quarkus:dev`: its raw-mode stdin hotkey handler would "
                + "conflict with dev mode's own. Solving without it; run the built application instead "
                + "(`java -jar target/quarkus-app/quarkus-run.jar`) to see the dashboard.");
        return true;
    }

    private void redirectOutput() {
        // Opened before assigning originalOut, so a failure here leaves restoreOutput() a no-op.
        PrintStream logStream;
        try {
            logStream = new PrintStream(new FileOutputStream(logFile.toFile()), true, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open the SolverConsole log file (%s).".formatted(logFile), e);
        }
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(logStream);
        System.setErr(logStream);
    }

    private void restoreOutput() {
        if (originalOut != null) {
            var redirected = System.out;
            System.setOut(originalOut);
            System.setErr(originalErr);
            redirected.close();
        }
    }

    /**
     * @return null if opening a terminal doesn't complete within {@value #TERMINAL_PROBE_TIMEOUT_SECONDS}s
     */
    private static Terminal tryBuildTerminal() {
        var future = new CompletableFuture<Terminal>();
        // A daemon thread: if the probe itself is stuck (rather than merely slow),
        // it must not keep the JVM alive,
        // nor must this method wait on it any longer than the timeout below.
        var probeThread = new Thread(() -> {
            try {
                future.complete(TerminalBuilder.builder().build());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, "solver-console-terminal-probe");
        probeThread.setDaemon(true);
        probeThread.start();
        try {
            return future.get(TERMINAL_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // If the probe does eventually complete, close what it opened instead of leaking it.
            future.whenComplete((lateTerminal, ignored) -> {
                if (lateTerminal != null) {
                    try {
                        lateTerminal.close();
                    } catch (IOException ignoredClose) {
                        // Best effort.
                    }
                }
            });
            return null;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw new UncheckedIOException("Could not open a terminal for the SolverConsole dashboard.", ioException);
            }
            throw new IllegalStateException("Could not open a terminal for the SolverConsole dashboard.", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while opening a terminal for the SolverConsole dashboard.", e);
        }
    }

    private void closeTerminalQuietly() {
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException ignored) {
                // Best effort.
            }
        }
    }

}
