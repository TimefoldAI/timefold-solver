package ai.timefold.solver.console.impl;

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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

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

/**
 * Renders a live terminal dashboard while a solver runs, driven by phase/step lifecycle events.
 * Owns the terminal, the {@code System.out}/{@code System.err} redirection and the
 * {@code ai.timefold.solver} JUL logger level for the duration of one {@link #start} /
 * {@link #stop} pair; see {@link ai.timefold.solver.console.api.SolverConsole} for the documented
 * single-call-at-a-time contract.
 *
 * @param <Solution_> the solution type
 */
public final class SolverDashboard<Solution_> extends PhaseLifecycleListenerAdapter<Solution_> {

    private static final String TIMEFOLD_LOGGER_NAME = "ai.timefold.solver";
    // One sample per repaint (REPAINT_PERIOD_MILLIS), so this is ~50 seconds of full-resolution
    // history before the first halving; halved back down to this (not dropped) once doubled, so
    // the sparkline always spans the whole run. Storage transiently ranges up to 2x this between
    // halvings.
    private static final int SPARKLINE_HISTORY_LIMIT = 200;
    private static final int PHASE_HISTORY_LIMIT = 50;
    private static final long REPAINT_PERIOD_MILLIS = 250L;
    private static final String CLEAR_SCREEN = "\033[H\033[2J"; // move cursor home, clear screen
    private static final String IDENTIFICATION = TimefoldSolverEnterpriseService.identifySolverVersion();

    private final Path logFile;
    private final Solver<Solution_> solver;
    private final SolverScope<Solution_> solverScope;

    private volatile String phaseName;
    private volatile long stepIndex = -1L;
    private volatile String stepScoreText = "n/a";
    // ponytail: em dash as a plain sentinel string rather than a shared constant; only used here and
    // in the two spots that reset it. Promote to a constant if a third caller shows up.
    private volatile String acceptedPercentText = "—";

    // Labels and per-level history are populated lazily on the first non-null best score, since the
    // score definition (and therefore the level count) isn't known before solving starts. Both are
    // only ever touched from inside paintFrame(), itself only ever invoked while holding this
    // dashboard's own monitor (see repaint()/stop()), so no separate lock is needed here.
    private List<String> scoreLevelLabels;
    private final List<ArrayDeque<Double>> bestScoreHistoryPerLevel = new ArrayList<>();
    private final List<String> finishedPhaseLines = Collections.synchronizedList(new ArrayList<>());

    // Written and read only from the solving thread (accumulated in stepEnded()/phaseEnded(),
    // read once in stop() right after solver.solve() returns on that same thread): no volatile
    // or synchronization needed, unlike the fields above that the repaint thread also reads.
    private long totalSteps;
    private long totalAcceptedMoveCount;
    private long totalSelectedMoveCount;

    private Terminal terminal;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private Level originalJulLevel;
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

    public void start() {
        failFastUnderQuarkusDevMode();
        try {
            terminal = TerminalBuilder.builder().build();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open a terminal for the SolverConsole dashboard.", e);
        }
        if (terminal.getType().startsWith(Terminal.TYPE_DUMB)) {
            closeTerminalQuietly();
            throw new IllegalStateException(
                    "SolverConsole requires an interactive terminal; it cannot render to a piped, "
                            + "redirected or non-interactive output. Run it directly in a terminal.");
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
        } catch (RuntimeException | Error e) {
            // Nothing acquired above (the redirected output, the raw terminal mode, the threads) may
            // outlive a failed start(): each would otherwise leak for the rest of the JVM's life.
            running = false;
            restoreOutput();
            closeTerminalQuietly();
            throw e;
        }
    }

    /**
     * @param solved false if {@code solver.solve()} threw rather than returning; skips the final
     *        summary box, since the solver scope it would report on may be left inconsistent
     */
    public void stop(boolean solved) {
        int terminalWidth;
        // Also entered by repaint() (the repaint thread and the WINCH resize handler): serializes
        // every terminal write and guarantees none of them can still be in flight once this method
        // closes the terminal below.
        synchronized (this) {
            running = false;
            repaintThread.interrupt();
            keyThread.interrupt();
            terminalWidth = terminal.getSize().getColumns();
            if (quitRequested) {
                // The dashboard box would otherwise linger above the summary printed below.
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
        System.out.println("Log written to " + logFile.toAbsolutePath());
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
        var acceptanceText = totalSelectedMoveCount == 0L
                ? "n/a"
                : String.format(Locale.US, "%.1f %%", 100.0 * totalAcceptedMoveCount / totalSelectedMoveCount);
        return new FinalSummary(
                title,
                finalScoreText(),
                totalSteps,
                solverScope.getTimeMillisSpent(),
                solverScope.getMoveEvaluationCount(),
                solverScope.getMoveEvaluationSpeed(),
                totalAcceptedMoveCount,
                acceptanceText,
                solverScope.getScoreCalculationCount());
    }

    // ************************************************************************
    // PhaseLifecycleListener
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseName = phaseScope.getPhaseId().simpleProducerName();
        stepIndex = -1L;
        stepScoreText = "n/a";
        acceptedPercentText = "—";
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        stepIndex = stepScope.getStepIndex();
        // AbstractStepScope.score stays unset for exhaustive search (its score lives on the
        // expanding node instead) and for any custom phase; fall back to n/a rather than NPE.
        InnerScore<?> stepScore = stepScope.getScore();
        if (stepScore == null && stepScope instanceof ExhaustiveSearchStepScope<Solution_> exhaustiveStepScope) {
            stepScore = exhaustiveStepScope.getStartingStepScore();
        }
        stepScoreText = stepScore == null ? "n/a" : stepScore.raw().toString();
        if (stepScope instanceof LocalSearchStepScope<Solution_> localSearchStepScope) {
            var accepted = localSearchStepScope.getAcceptedMoveCount();
            var selected = localSearchStepScope.getSelectedMoveCount();
            if (accepted != null && selected != null) {
                // Construction heuristic steps have a selected count but no acceptor, so no accepted
                // count; only local search steps have both. Summing selected-without-accepted moves
                // into these running totals would understate the final acceptance percentage.
                totalAcceptedMoveCount += accepted;
                totalSelectedMoveCount += selected;
            }
            acceptedPercentText = (accepted == null || selected == null || selected == 0L)
                    ? "—"
                    : String.format(Locale.US, "%.1f %%", 100.0 * accepted / selected);
        } else {
            acceptedPercentText = "—";
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        totalSteps += phaseScope.getNextStepIndex(); // Steps are re-indexed from 0 per phase.
        // ponytail: phaseScope.endingSystemTimeMillis is not set yet when this listener fires
        // (DefaultLocalSearchPhase.phaseEnded() fires listeners before calling phaseScope.endingNow()),
        // so getPhaseMoveEvaluationSpeed()/getPhaseTimeMillisSpent() would NPE here. Compute the speed
        // from the "up to now" variants instead, which only need startingSystemTimeMillis.
        var phaseTimeMillisSpent = phaseScope.calculatePhaseTimeMillisSpentUpToNow();
        var phaseMoveEvaluationSpeed = phaseScope.getPhaseMoveEvaluationCount() * 1000L
                / (phaseTimeMillisSpent == 0L ? 1L : phaseTimeMillisSpent);
        var line = String.format(Locale.US, "%s  %s ended: %d steps, %,d/s, score %s",
                DashboardRenderer.formatElapsed(phaseScope.calculateSolverTimeMillisSpentUpToNow()),
                phaseScope.getPhaseId().simpleProducerName(),
                phaseScope.getNextStepIndex(),
                phaseMoveEvaluationSpeed,
                phaseScope.getBestScore().raw());
        synchronized (finishedPhaseLines) {
            finishedPhaseLines.add(line);
            while (finishedPhaseLines.size() > PHASE_HISTORY_LIMIT) {
                finishedPhaseLines.remove(0);
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

    // Synchronized so the repaint thread, the WINCH resize handler and stop()'s own final frame can
    // never write to the terminal concurrently or after stop() has closed it: the running check runs
    // under the same lock stop() takes before closing, so a call arriving after stop() started is
    // guaranteed to see running == false and return without touching the terminal.
    // ponytail: one lock, four frames a second; revisit if the repaint cadence ever needs to be
    // much higher.
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
     * Samples once per repaint (a fixed wall-clock cadence, {@value #REPAINT_PERIOD_MILLIS} ms)
     * rather than once per step, so the sparkline's length always reflects elapsed time, not step
     * count: a phase doing thousands of fast steps per second no longer dominates the chart while a
     * phase doing a handful of slow steps per second is squeezed to almost nothing, even though both
     * took the same real time. Sampling unconditionally (not only on improvement) means a genuine
     * plateau still advances the chart as flat, instead of the timeline silently skipping over it.
     */
    void recordBestScoreSample() {
        var bestScore = solverScope.getBestScore();
        if (bestScore == null) {
            return; // Solving hasn't started yet.
        }
        var levels = bestScore.raw().toLevelDoubles();
        if (scoreLevelLabels == null) {
            // The score definition (and therefore the level count) isn't known before solving starts,
            // so both are populated here, on the first sample, rather than in the constructor.
            scoreLevelLabels = buildScoreLevelLabels(levels.length);
            for (var i = 0; i < levels.length; i++) {
                bestScoreHistoryPerLevel.add(new ArrayDeque<>());
            }
        }
        for (var i = 0; i < levels.length; i++) {
            var history = bestScoreHistoryPerLevel.get(i);
            history.addLast(levels[i]);
            if (history.size() >= 2 * SPARKLINE_HISTORY_LIMIT) {
                // Halve only once the history has grown to exactly double the limit, so every bucket
                // merges exactly 2:1 with no rounding remainder. Squeezing down by 1 every single step
                // instead (a barely-over-capacity "201 into 200" downsample) always lands its one
                // uneven bucket at the tail, turning the newest bucket into a decaying exponential
                // moving average that stops responding to new data while older buckets never change.
                var compacted = DashboardRenderer.downsample(List.copyOf(history), SPARKLINE_HISTORY_LIMIT);
                history.clear();
                history.addAll(compacted);
            }
        }
    }

    /**
     * Indexed the same way as {@link #bestScoreHistoryPerLevel}, one label per score level (e.g.
     * {@code hard}, {@code medium}, {@code soft}), the same way {@code ProblemBenchmarkResult} in
     * {@code tools/benchmark} labels score levels in its own reports.
     */
    private List<String> buildScoreLevelLabels(int levelCount) {
        var rawLabels = solverScope.getScoreDefinition().getLevelLabels();
        var labels = new ArrayList<String>(levelCount);
        for (var rawLabel : rawLabels) {
            labels.add(rawLabel.endsWith(" score") ? rawLabel.substring(0, rawLabel.length() - " score".length())
                    : rawLabel);
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
        return new DashboardSnapshot(
                elapsedMillis,
                phaseName,
                stepIndex,
                bestScore == null ? "n/a" : bestScore.raw().toString(),
                stepScoreText,
                acceptedPercentText,
                solverScope.getMoveEvaluationSpeed(),
                solverScope.getMoveEvaluationCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.entityCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.variableCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.approximateValueCount(),
                problemSizeStatistics == null ? null : problemSizeStatistics.approximateProblemScaleAsFormattedString(),
                levelLabelsSnapshot,
                List.copyOf(historySnapshot),
                phaseLinesSnapshot);
    }

    // ************************************************************************
    // Environment guards
    // ************************************************************************

    private void failFastUnderQuarkusDevMode() {
        try {
            var launchModeClass = Class.forName("io.quarkus.runtime.LaunchMode");
            var current = launchModeClass.getMethod("current").invoke(null);
            if ("DEVELOPMENT".equals(current.toString())) {
                throw new IllegalStateException(
                        "SolverConsole cannot run under `quarkus:dev`: dev mode's own raw-mode stdin hotkey "
                                + "handler conflicts with the dashboard's. Run the built application instead "
                                + "(`java -jar target/quarkus-app/quarkus-run.jar`).");
            }
        } catch (ClassNotFoundException e) {
            // Not a Quarkus application; nothing to check.
        } catch (ReflectiveOperationException e) {
            // Quarkus is present but its LaunchMode API changed shape; don't block on a best-effort check.
        }
    }

    private void redirectOutput() {
        // Opened before anything is assigned, so a failure here leaves originalOut null and
        // restoreOutput() a no-op instead of closing the real System.out it never actually replaced.
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
        var timefoldLogger = Logger.getLogger(TIMEFOLD_LOGGER_NAME);
        originalJulLevel = timefoldLogger.getLevel();
        timefoldLogger.setLevel(Level.OFF);
    }

    private void restoreOutput() {
        if (originalOut != null) {
            var redirected = System.out;
            System.setOut(originalOut);
            System.setErr(originalErr);
            redirected.close();
            Logger.getLogger(TIMEFOLD_LOGGER_NAME).setLevel(originalJulLevel);
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
