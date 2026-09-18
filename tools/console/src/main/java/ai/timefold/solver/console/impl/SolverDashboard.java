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
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
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

    private volatile Solver<Solution_> solver;
    private volatile SolverScope<Solution_> solverScope;
    private volatile String phaseName;
    private volatile long stepIndex = -1L;
    private volatile String stepScoreText = "n/a";
    // ponytail: em dash as a plain sentinel string rather than a shared constant; only used here and
    // in the two spots that reset it. Promote to a constant if a third caller shows up.
    private volatile String acceptedPercentText = "—";

    private final ArrayDeque<Double> bestScoreHistory = new ArrayDeque<>();
    private final List<String> finishedPhaseLines = Collections.synchronizedList(new ArrayList<>());

    private Terminal terminal;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private Level originalJulLevel;
    private Thread repaintThread;
    private Thread keyThread;
    private volatile boolean running;
    private volatile boolean quitRequested;

    public SolverDashboard(Path logFile) {
        this.logFile = logFile;
    }

    // ************************************************************************
    // Lifecycle, called by SolverConsole
    // ************************************************************************

    public void start(Solver<Solution_> solver, SolverScope<Solution_> solverScope) {
        this.solver = solver;
        this.solverScope = solverScope;
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
    }

    public void stop() {
        running = false;
        repaintThread.interrupt();
        keyThread.interrupt();
        if (quitRequested) {
            // The dashboard box would otherwise linger above the summary printed below.
            var writer = terminal.writer();
            writer.print(CLEAR_SCREEN);
            writer.flush();
        } else {
            repaint();
        }
        restoreOutput();
        closeTerminalQuietly();
        // Printed after output is restored and the terminal is closed, so it reaches the real console.
        System.out.println();
        System.out.println("Solved: " + finalScoreText());
        System.out.println("Log written to " + logFile.toAbsolutePath());
    }

    private String finalScoreText() {
        var bestScore = solverScope.getBestScore();
        return bestScore == null ? "n/a" : bestScore.raw().toString();
    }

    // ************************************************************************
    // PhaseLifecycleListener
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseName = phaseScope.getPhaseId().simpleProducerName();
        stepIndex = -1L;
        acceptedPercentText = "—";
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        stepIndex = stepScope.getStepIndex();
        stepScoreText = stepScope.getScore().raw().toString();
        if (stepScope instanceof LocalSearchStepScope<Solution_> localSearchStepScope) {
            var accepted = localSearchStepScope.getAcceptedMoveCount();
            var selected = localSearchStepScope.getSelectedMoveCount();
            acceptedPercentText = (accepted == null || selected == null || selected == 0L)
                    ? "—"
                    : "%.1f %%".formatted(100.0 * accepted / selected);
        } else {
            acceptedPercentText = "—";
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        // ponytail: phaseScope.endingSystemTimeMillis is not set yet when this listener fires
        // (DefaultLocalSearchPhase.phaseEnded() fires listeners before calling phaseScope.endingNow()),
        // so getPhaseMoveEvaluationSpeed()/getPhaseTimeMillisSpent() would NPE here. Compute the speed
        // from the "up to now" variants instead, which only need startingSystemTimeMillis.
        var phaseTimeMillisSpent = phaseScope.calculatePhaseTimeMillisSpentUpToNow();
        var phaseMoveEvaluationSpeed = phaseScope.getPhaseMoveEvaluationCount() * 1000L
                / (phaseTimeMillisSpent == 0L ? 1L : phaseTimeMillisSpent);
        var line = "%s  %s ended: %d steps, %,d/s, score %s".formatted(
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

    private void repaint() {
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
    private void recordBestScoreSample() {
        var bestScore = solverScope.getBestScore();
        if (bestScore == null) {
            return; // Solving hasn't started yet.
        }
        synchronized (bestScoreHistory) {
            var levels = bestScore.raw().toLevelDoubles();
            bestScoreHistory.addLast(levels[levels.length - 1]);
            if (bestScoreHistory.size() >= 2 * SPARKLINE_HISTORY_LIMIT) {
                // Halve only once the history has grown to exactly double the limit, so every bucket
                // merges exactly 2:1 with no rounding remainder. Squeezing down by 1 every single step
                // instead (a barely-over-capacity "201 into 200" downsample) always lands its one
                // uneven bucket at the tail, turning the newest bucket into a decaying exponential
                // moving average that stops responding to new data while older buckets never change.
                var compacted = DashboardRenderer.downsample(List.copyOf(bestScoreHistory), SPARKLINE_HISTORY_LIMIT);
                bestScoreHistory.clear();
                bestScoreHistory.addAll(compacted);
            }
        }
    }

    private DashboardSnapshot buildSnapshot() {
        List<Double> historySnapshot;
        synchronized (bestScoreHistory) {
            historySnapshot = List.copyOf(bestScoreHistory);
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
                solverScope.getScoreCalculationCount(),
                solverScope.getScoreCalculationSpeed(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.entityCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.variableCount(),
                problemSizeStatistics == null ? 0L : problemSizeStatistics.approximateValueCount(),
                problemSizeStatistics == null ? null : problemSizeStatistics.approximateProblemScaleAsFormattedString(),
                historySnapshot,
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
        originalOut = System.out;
        originalErr = System.err;
        try {
            var logStream = new PrintStream(new FileOutputStream(logFile.toFile()), true, StandardCharsets.UTF_8);
            System.setOut(logStream);
            System.setErr(logStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open the SolverConsole log file (%s).".formatted(logFile), e);
        }
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
        }
        Logger.getLogger(TIMEFOLD_LOGGER_NAME).setLevel(originalJulLevel);
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
