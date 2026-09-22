package ai.timefold.solver.console.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a {@link DashboardSnapshot} into the lines of one frame. No I/O, no terminal access, no
 * shared state — a pure function, safe to unit test directly.
 */
final class DashboardRenderer {

    private static final char[] SPARK_LEVELS =
            { '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█' };
    private static final int MIN_WIDTH = 40;
    private static final int MAX_WIDTH = 120;
    private static final int MIN_TITLE_LENGTH = "Timefold".length();
    private static final String SPARKLINE_LABEL = "  best score over time";

    private DashboardRenderer() {
    }

    static List<String> render(DashboardSnapshot snapshot, int width, int height, String identification) {
        var frameWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width));
        var contentWidth = frameWidth - 4; // "│ " + content + " │"

        var body = new ArrayList<String>();
        var phaseLine = snapshot.phaseName() == null ? "Waiting to start..." : "Phase: " + snapshot.phaseName();
        body.add(content(phaseLine, "[q] to stop", contentWidth));
        body.add(content("Best: " + snapshot.bestScoreText(), "", contentWidth));
        body.add(content("Step: " + snapshot.stepScoreText(), "", contentWidth));
        body.addAll(sparklineRows(snapshot.scoreLevelLabels(), snapshot.bestScoreHistoryByLevel(), contentWidth));

        // Both columns' labels and values are padded to the wider of the two rows, so "Speed:" and
        // "Accepted:" (and their values) start at the same column in both rows regardless of which
        // one happens to be longer on a given frame.
        var stepsText = format(snapshot.stepIndex() + 1);
        var movesEvalText = format(snapshot.moveEvaluationCount());
        var leftLabelWidth = Math.max("Steps:".length(), "Moves eval:".length());
        var leftValueWidth = Math.max(stepsText.length(), movesEvalText.length());

        var speedText = format(snapshot.moveEvaluationSpeed()) + "/s";
        var acceptedText = snapshot.acceptedPercentText();
        var rightLabelWidth = Math.max("Speed:".length(), "Accepted:".length());
        var rightValueWidth = Math.max(speedText.length(), acceptedText.length());

        var stepsColumn = statColumn("Steps:", stepsText, leftLabelWidth, leftValueWidth);
        var movesEvalColumn = statColumn("Moves eval:", movesEvalText, leftLabelWidth, leftValueWidth);
        var speedColumn = statColumn("Speed:", speedText, rightLabelWidth, rightValueWidth);
        var acceptedColumn = statColumn("Accepted:", acceptedText, rightLabelWidth, rightValueWidth);
        // borderedLine() only guarantees a single space between the two columns, so anything wider
        // than contentWidth minus that one space gets silently cut mid-value; both rows share the
        // same column widths, so checking one row covers both.
        if (stepsColumn.length() + 1 + speedColumn.length() <= contentWidth) {
            body.add(content(stepsColumn, speedColumn, contentWidth));
            body.add(content(movesEvalColumn, acceptedColumn, contentWidth));
        } else {
            body.add(content(stepsColumn, "", contentWidth));
            body.add(content(speedColumn, "", contentWidth));
            body.add(content(movesEvalColumn, "", contentWidth));
            body.add(content(acceptedColumn, "", contentWidth));
        }

        // Borders and the problem size line always fit; the body can grow past the terminal height
        // with enough score levels or a narrow terminal (the stat rows stack to four), so phase
        // history gets whatever is left of the height budget, dropping the oldest lines first, and
        // can itself shrink to nothing.
        var alwaysPresent = 3 /* top border + problem size line + bottom border */ + body.size();
        var historyBudget = Math.max(0, height - alwaysPresent - 1 /* separator, reserved only if shown */);
        var history = snapshot.finishedPhaseLines();
        var shown = Math.min(historyBudget, history.size());
        var visibleHistory = history.subList(history.size() - shown, history.size());

        var lines = new ArrayList<String>();
        lines.add(topBorder(frameWidth, formatElapsed(snapshot.elapsedMillis()), identification));
        lines.add(problemSizeLine(snapshot, contentWidth));
        for (var historyLine : visibleHistory) {
            lines.add(content(historyLine, "", contentWidth));
        }
        if (!visibleHistory.isEmpty()) {
            lines.add(separator(frameWidth));
        }
        lines.addAll(body);
        lines.add(bottomBorder(frameWidth));
        return center(lines, frameWidth, width);
    }

    private static List<String> center(List<String> lines, int frameWidth, int terminalWidth) {
        var leftPad = Math.max(0, (terminalWidth - frameWidth) / 2);
        if (leftPad == 0) {
            return lines;
        }
        var pad = " ".repeat(leftPad);
        var centered = new ArrayList<String>(lines.size());
        for (var line : lines) {
            centered.add(pad + line);
        }
        return centered;
    }

    /**
     * One sparkline row per score level (e.g. {@code hard} and {@code soft}), each labelled and
     * indented to the widest label, plus one caption row underneath indented to the bars. Plotting
     * the levels separately, rather than only the softest one, keeps a hard-constraint repair paid
     * for with a soft-score cost from reading as a regression: each level's own row only ever moves
     * the direction that level's score actually moved.
     */
    private static List<String> sparklineRows(List<String> levelLabels, List<List<Double>> historyByLevel,
            int contentWidth) {
        if (levelLabels.isEmpty()) {
            return List.of(content(sparkline(List.of(), Math.max(1, contentWidth - SPARKLINE_LABEL.length()))
                    + SPARKLINE_LABEL, "", contentWidth));
        }
        var labelWidth = levelLabels.stream().mapToInt(String::length).max().orElseThrow();
        var barWidth = Math.max(1, contentWidth - labelWidth - 2);
        var rows = new ArrayList<String>(levelLabels.size() + 1);
        for (var i = 0; i < levelLabels.size(); i++) {
            var bars = sparkline(historyByLevel.get(i), barWidth);
            rows.add(content(padRight(levelLabels.get(i), labelWidth) + "  " + bars, "", contentWidth));
        }
        rows.add(content(" ".repeat(labelWidth + 2) + "best score over time", "", contentWidth));
        return rows;
    }

    private static String problemSizeLine(DashboardSnapshot snapshot, int contentWidth) {
        var text = snapshot.approximateProblemScaleText() == null
                ? "Problem: Waiting to start..." // mirrors the phaseLine placeholder wording above
                : "Problem: %s entities, %s variables, %s values, scale %s".formatted(
                        format(snapshot.entityCount()), format(snapshot.variableCount()),
                        format(snapshot.approximateValueCount()), snapshot.approximateProblemScaleText());
        return content(text, "", contentWidth);
    }

    private static String content(String left, String right, int contentWidth) {
        return borderedLine("│", left, right, contentWidth);
    }

    private static String doubleContent(String left, String right, int contentWidth) {
        return borderedLine("║", left, right, contentWidth);
    }

    private static String borderedLine(String vertical, String left, String right, int contentWidth) {
        var text = right.isEmpty() ? left
                : left + " ".repeat(Math.max(1, contentWidth - left.length() - right.length())) + right;
        if (text.length() > contentWidth) {
            text = text.substring(0, contentWidth);
        } else {
            text = text + " ".repeat(contentWidth - text.length());
        }
        return vertical + " " + text + " " + vertical;
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        var totalPad = width - text.length();
        var leftPad = totalPad / 2;
        return " ".repeat(leftPad) + text + " ".repeat(totalPad - leftPad);
    }

    private static String statColumn(String label, String value, int labelWidth, int valueWidth) {
        return padRight(label, labelWidth) + " " + padRight(value, valueWidth);
    }

    private static String padRight(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    private static String topBorder(int frameWidth, String timeText, String identification) {
        var rightLabel = " " + timeText + " ";
        // -2 corners, -1 minimum dash, -2 title's own padding spaces.
        var maxTitleTextLength = Math.max(MIN_TITLE_LENGTH, frameWidth - 5 - rightLabel.length());
        var titleText = identification.length() > maxTitleTextLength
                ? identification.substring(0, maxTitleTextLength)
                : identification;
        var title = " " + titleText + " ";
        var dashCount = Math.max(1, frameWidth - 2 - title.length() - rightLabel.length());
        return "┌" + title + "─".repeat(dashCount) + rightLabel + "┐";
    }

    private static String bottomBorder(int frameWidth) {
        return "└" + "─".repeat(Math.max(0, frameWidth - 2)) + "┘";
    }

    private static String separator(int frameWidth) {
        return "├" + "─".repeat(Math.max(0, frameWidth - 2)) + "┤";
    }

    /**
     * Renders the one-shot report printed after the live dashboard closes: a double-line box,
     * visually distinct from the live dashboard's single-line box, since it reports on a finished
     * run rather than an ongoing one.
     */
    static List<String> renderFinalSummary(FinalSummary summary, int width) {
        var frameWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width));
        var contentWidth = frameWidth - 4;

        var lines = new ArrayList<String>();
        lines.add(doubleTopBorder(frameWidth));
        lines.add(doubleContent(centerText(summary.title(), contentWidth), "", contentWidth));
        lines.add(doubleSeparator(frameWidth));
        lines.add(doubleContent("  Final Score:", summary.finalScoreText(), contentWidth));
        lines.add(doubleContent("  Steps:", format(summary.steps()), contentWidth));
        lines.add(doubleContent("  Solve Time:", formatElapsed(summary.solveTimeMillis()), contentWidth));
        lines.add(doubleContent("  Moves Evaluated:", format(summary.movesEvaluated()), contentWidth));
        lines.add(doubleContent("  Moves/s:", format(summary.movesEvaluationSpeed()), contentWidth));
        lines.add(doubleContent("  Moves Accepted:", format(summary.movesAccepted()), contentWidth));
        lines.add(doubleContent("  Acceptance:", summary.acceptanceText(), contentWidth));
        lines.add(doubleContent("  Score Calcs:", format(summary.scoreCalculationCount()), contentWidth));
        lines.add(doubleBottomBorder(frameWidth));
        return center(lines, frameWidth, width);
    }

    private static String doubleTopBorder(int frameWidth) {
        return "╔" + "═".repeat(Math.max(0, frameWidth - 2)) + "╗";
    }

    private static String doubleBottomBorder(int frameWidth) {
        return "╚" + "═".repeat(Math.max(0, frameWidth - 2)) + "╝";
    }

    private static String doubleSeparator(int frameWidth) {
        return "╠" + "═".repeat(Math.max(0, frameWidth - 2)) + "╣";
    }

    private static String sparkline(List<Double> history, int width) {
        if (history.isEmpty()) {
            return " ".repeat(width);
        }
        // Whole-run view: compress the full history into exactly `width` columns rather than only
        // showing its most recent tail. Short histories just fill in from the left instead.
        var downsampled = downsample(history, width);
        // Normalize against the downsampled (displayed) values, not the raw history: averaging
        // narrows a bucket's range versus its raw samples, so a min/max taken from raw history can
        // permanently outrange every later bucket after one old, sharp swing, crushing everything
        // since — including genuine, smaller-scale, ongoing progress — toward a single flat level.
        var min = downsampled.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        var max = downsampled.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        var range = max - min == 0.0 ? 1.0 : max - min;
        var builder = new StringBuilder();
        for (var value : downsampled) {
            var level = (int) (((value - min) * (SPARK_LEVELS.length - 1)) / range);
            builder.append(SPARK_LEVELS[Math.max(0, Math.min(SPARK_LEVELS.length - 1, level))]);
        }
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    /**
     * Compresses {@code values} down to exactly {@code targetSize} entries by averaging
     * proportional slices, so the result always fills the full requested width — never fewer
     * entries, even when {@code values} only just barely exceeds {@code targetSize}. Returns
     * {@code values} unchanged if it already fits. Shared between rendering (compress to the
     * current terminal width) and {@link SolverDashboard}'s history bookkeeping (compress to stay
     * within its retention cap while still spanning the whole run).
     * <p>
     * A fixed-width chunking scheme (grouping every {@code ceil(size / targetSize)} raw samples)
     * was tried instead, to keep already-computed groups from reshuffling as more data streams in.
     * It does not work here: an integer group width can only take values 1, 2, 3, ... and jumps
     * straight from 1 to 2 the moment {@code values.size()} first exceeds {@code targetSize} by
     * even one sample — instantly halving the visible bucket count right as the sparkline first
     * fills the terminal. Proportional slicing has no such cliff. Its own downside (bucket
     * boundaries shift slightly as {@code values} grows) is far less visible now that
     * {@link SolverDashboard} samples once per repaint (a slow, steady cadence) rather than once
     * per solver step (bursty and fast), which was the original reason to move away from it.
     */
    static List<Double> downsample(List<Double> values, int targetSize) {
        if (values.size() <= targetSize) {
            return values;
        }
        var downsampled = new ArrayList<Double>(targetSize);
        for (var bucket = 0; bucket < targetSize; bucket++) {
            var start = bucket * values.size() / targetSize;
            var end = Math.max(start + 1, (bucket + 1) * values.size() / targetSize);
            var sum = 0.0;
            for (var i = start; i < end; i++) {
                sum += values.get(i);
            }
            downsampled.add(sum / (end - start));
        }
        return downsampled;
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    static String formatElapsed(long millis) {
        var duration = Duration.ofMillis(Math.max(0L, millis));
        var hours = duration.toHours();
        var minutes = duration.toMinutesPart();
        var seconds = duration.toSecondsPart();
        return hours > 0 ? "%d:%02d:%02d".formatted(hours, minutes, seconds) : "%d:%02d".formatted(minutes, seconds);
    }

}
