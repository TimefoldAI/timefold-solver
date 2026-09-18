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

    private DashboardRenderer() {
    }

    static List<String> render(DashboardSnapshot snapshot, int width, int height) {
        var frameWidth = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width));
        var contentWidth = frameWidth - 4; // "│ " + content + " │"

        var body = new ArrayList<String>();
        var phaseLine = snapshot.phaseName() == null ? "Waiting to start..." : "Phase  " + snapshot.phaseName();
        body.add(content(phaseLine, "[q] to stop", contentWidth));
        body.add(content("Best   " + snapshot.bestScoreText(), "", contentWidth));
        body.add(content("Step   " + snapshot.stepScoreText(), "", contentWidth));
        body.add(content(sparkline(snapshot.bestScoreHistory(), Math.max(1, contentWidth - 20))
                + "  best score over time", "", contentWidth));
        body.add(twoColumns("Steps", format(snapshot.stepIndex() + 1), "Speed",
                format(snapshot.moveEvaluationSpeed()) + "/s", contentWidth));
        body.add(twoColumns("Moves eval", format(snapshot.moveEvaluationCount()), "Accepted",
                snapshot.acceptedPercentText(), contentWidth));
        body.add(twoColumns("Score calc", format(snapshot.scoreCalculationCount()), "Entities",
                format(snapshot.entityCount()), contentWidth));

        // Borders and body always fit; phase history gets whatever is left of the height budget,
        // dropping the oldest lines first.
        var alwaysPresent = 2 /* top + bottom border */ + body.size();
        var historyBudget = Math.max(0, height - alwaysPresent - 1 /* separator, reserved only if shown */);
        var history = snapshot.finishedPhaseLines();
        var shown = Math.min(historyBudget, history.size());
        var visibleHistory = history.subList(history.size() - shown, history.size());

        var lines = new ArrayList<String>();
        lines.add(topBorder(frameWidth, formatElapsed(snapshot.elapsedMillis())));
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

    private static String content(String left, String right, int contentWidth) {
        var text = right.isEmpty() ? left
                : left + " ".repeat(Math.max(1, contentWidth - left.length() - right.length())) + right;
        if (text.length() > contentWidth) {
            text = text.substring(0, contentWidth);
        } else {
            text = text + " ".repeat(contentWidth - text.length());
        }
        return "│ " + text + " │";
    }

    private static String twoColumns(String label1, String value1, String label2, String value2, int contentWidth) {
        var left = "%-10s %s".formatted(label1, value1);
        var right = "%-10s %s".formatted(label2, value2);
        return content(left, right, contentWidth);
    }

    private static String topBorder(int frameWidth, String timeText) {
        var title = " Timefold Solver ";
        var rightLabel = " " + timeText + " ";
        var dashCount = Math.max(1, frameWidth - 2 - title.length() - rightLabel.length());
        return "┌" + title + "─".repeat(dashCount) + rightLabel + "┐";
    }

    private static String bottomBorder(int frameWidth) {
        return "└" + "─".repeat(Math.max(0, frameWidth - 2)) + "┘";
    }

    private static String separator(int frameWidth) {
        return "├" + "─".repeat(Math.max(0, frameWidth - 2)) + "┤";
    }

    private static String sparkline(List<Long> history, int width) {
        if (history.isEmpty()) {
            return " ".repeat(width);
        }
        var min = history.stream().mapToLong(Long::longValue).min().orElseThrow();
        var max = history.stream().mapToLong(Long::longValue).max().orElseThrow();
        var range = Math.max(1L, max - min);
        var recent = history.subList(Math.max(0, history.size() - width), history.size());
        var builder = new StringBuilder();
        for (var value : recent) {
            var level = (int) (((value - min) * (SPARK_LEVELS.length - 1)) / range);
            builder.append(SPARK_LEVELS[Math.max(0, Math.min(SPARK_LEVELS.length - 1, level))]);
        }
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
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
