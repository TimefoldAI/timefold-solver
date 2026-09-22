package ai.timefold.solver.console.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link DashboardSnapshot} into the lines of one frame.
 * No I/O, no terminal access, no shared state —
 * a pure function, safe to unit test directly.
 */
final class DashboardRenderer {

    private static final char[] SPARK_LEVELS =
            { '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█' };
    private static final int MIN_WIDTH = 40;
    private static final int MAX_WIDTH = 120;
    private static final int MIN_TITLE_LENGTH = "Timefold".length();
    private static final String BEST_SCORE_CAPTION = "best score over time";
    private static final String SPARKLINE_LABEL = "  " + BEST_SCORE_CAPTION;
    private static final String WAITING_TO_START = "Waiting to start...";
    private static final String STEPS_LABEL = "Steps:";
    private static final String MOVES_EVAL_LABEL = "Moves eval:";
    private static final String SPEED_LABEL = "Speed:";
    private static final String ACCEPTED_LABEL = "Accepted:";
    private static final String HORIZONTAL_LINE = "─";
    private static final String DOUBLE_HORIZONTAL_LINE = "═";

    private DashboardRenderer() {
    }

    static List<String> render(DashboardSnapshot snapshot, int width, int height, String identification) {
        var frameWidth = Math.clamp(width, MIN_WIDTH, MAX_WIDTH);
        var contentWidth = frameWidth - 4; // "│ " + content + " │"

        var body = new ArrayList<String>();
        var phaseLine = snapshot.phaseName() == null ? WAITING_TO_START : "Phase: " + snapshot.phaseName();
        body.add(content(phaseLine, "[q] to stop", contentWidth));
        body.add(content("Best: " + snapshot.bestScoreText(), "", contentWidth));
        body.add(content("Step: " + snapshot.stepScoreText(), "", contentWidth));
        body.addAll(sparklineRows(snapshot.scoreLevelLabels(), snapshot.bestScoreHistoryByLevel(), contentWidth));

        // Padded to the wider row so both columns align regardless of which value is longer.
        var stepsText = format(snapshot.stepIndex() + 1);
        var movesEvalText = format(snapshot.moveEvaluationCount());
        var leftLabelWidth = Math.max(STEPS_LABEL.length(), MOVES_EVAL_LABEL.length());
        var leftValueWidth = Math.max(stepsText.length(), movesEvalText.length());

        var speedText = format(snapshot.moveEvaluationSpeed()) + "/s";
        var acceptedText = snapshot.acceptedPercentText();
        var rightLabelWidth = Math.max(SPEED_LABEL.length(), ACCEPTED_LABEL.length());
        var rightValueWidth = Math.max(speedText.length(), acceptedText.length());

        var stepsColumn = statColumn(STEPS_LABEL, stepsText, leftLabelWidth, leftValueWidth);
        var movesEvalColumn = statColumn(MOVES_EVAL_LABEL, movesEvalText, leftLabelWidth, leftValueWidth);
        var speedColumn = statColumn(SPEED_LABEL, speedText, rightLabelWidth, rightValueWidth);
        var acceptedColumn = statColumn(ACCEPTED_LABEL, acceptedText, rightLabelWidth, rightValueWidth);
        // Both rows share column widths, so checking one covers both; otherwise stack into 4 single-column rows.
        if (stepsColumn.length() + 1 + speedColumn.length() <= contentWidth) {
            body.add(content(stepsColumn, speedColumn, contentWidth));
            body.add(content(movesEvalColumn, acceptedColumn, contentWidth));
        } else {
            body.add(content(stepsColumn, "", contentWidth));
            body.add(content(speedColumn, "", contentWidth));
            body.add(content(movesEvalColumn, "", contentWidth));
            body.add(content(acceptedColumn, "", contentWidth));
        }

        // Phase history gets whatever height is left after borders and body, dropping oldest lines first.
        var alwaysPresent = 3 + body.size(); // top border + problem size line + bottom border
        var historyBudget = Math.max(0, height - alwaysPresent - 1); // separator, reserved only if shown
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
     * One sparkline row per score level (e.g. {@code hard}, {@code soft});
     * plotted separately so a hard-constraint repair paid for with a soft-score cost
     * doesn't read as a regression on either row.
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
        rows.add(content(" ".repeat(labelWidth + 2) + BEST_SCORE_CAPTION, "", contentWidth));
        return rows;
    }

    private static String problemSizeLine(DashboardSnapshot snapshot, int contentWidth) {
        var text = snapshot.approximateProblemScaleText() == null
                ? "Problem: " + WAITING_TO_START
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
        return "┌" + title + HORIZONTAL_LINE.repeat(dashCount) + rightLabel + "┐";
    }

    private static String bottomBorder(int frameWidth) {
        return "└" + HORIZONTAL_LINE.repeat(Math.max(0, frameWidth - 2)) + "┘";
    }

    private static String separator(int frameWidth) {
        return "├" + HORIZONTAL_LINE.repeat(Math.max(0, frameWidth - 2)) + "┤";
    }

    /**
     * Renders the one-shot report printed after the live dashboard closes,
     * as a double-line box to visually distinguish it from the single-line live dashboard.
     */
    static List<String> renderFinalSummary(FinalSummary summary, int width) {
        var frameWidth = Math.clamp(width, MIN_WIDTH, MAX_WIDTH);
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
        return "╔" + DOUBLE_HORIZONTAL_LINE.repeat(Math.max(0, frameWidth - 2)) + "╗";
    }

    private static String doubleBottomBorder(int frameWidth) {
        return "╚" + DOUBLE_HORIZONTAL_LINE.repeat(Math.max(0, frameWidth - 2)) + "╝";
    }

    private static String doubleSeparator(int frameWidth) {
        return "╠" + DOUBLE_HORIZONTAL_LINE.repeat(Math.max(0, frameWidth - 2)) + "╣";
    }

    private static String sparkline(List<Double> history, int width) {
        if (history.isEmpty()) {
            return " ".repeat(width);
        }
        // Whole-run view: compress history into exactly `width` columns instead of showing only the recent tail.
        var downsampled = downsample(history, width);
        // Normalized against the downsampled values, not raw history: a raw min/max can get stuck on one old
        // sharp swing and crush all later, genuine progress toward a flat level.
        var min = downsampled.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        var max = downsampled.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        var range = max - min == 0.0 ? 1.0 : max - min;
        var builder = new StringBuilder();
        for (var value : downsampled) {
            var level = (int) (((value - min) * (SPARK_LEVELS.length - 1)) / range);
            builder.append(SPARK_LEVELS[Math.clamp(level, 0, SPARK_LEVELS.length - 1)]);
        }
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }

    /**
     * Compresses {@code values} to exactly {@code targetSize} entries by averaging proportional slices
     * (never fewer, even when barely over {@code targetSize});
     * returns {@code values} unchanged if it already fits.
     * Shared by rendering (compress to terminal width) and {@link SolverDashboard}'s history
     * (compress to its retention cap).
     * Proportional slicing, not fixed-width chunking, avoids a cliff
     * where an integer group width jumps from 1 to 2 the moment {@code values.size()} first exceeds {@code targetSize}.
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
        return String.format("%,d", value);
    }

    static String formatElapsed(long millis) {
        var duration = Duration.ofMillis(Math.max(0L, millis));
        var hours = duration.toHours();
        var minutes = duration.toMinutesPart();
        var seconds = duration.toSecondsPart();
        return hours > 0 ? "%d:%02d:%02d".formatted(hours, minutes, seconds) : "%d:%02d".formatted(minutes, seconds);
    }

}
