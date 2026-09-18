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
        var phaseLine = snapshot.phaseName() == null ? "Waiting to start..." : "Phase  " + snapshot.phaseName();
        body.add(content(phaseLine, "[q] to stop", contentWidth));
        body.add(content("Best   " + snapshot.bestScoreText(), "", contentWidth));
        body.add(content("Step   " + snapshot.stepScoreText(), "", contentWidth));
        body.add(content(sparkline(snapshot.bestScoreHistory(), Math.max(1, contentWidth - SPARKLINE_LABEL.length()))
                + SPARKLINE_LABEL, "", contentWidth));
        body.add(twoColumns("Steps", format(snapshot.stepIndex() + 1), "Speed",
                format(snapshot.moveEvaluationSpeed()) + "/s", contentWidth));
        body.add(twoColumns("Moves eval", format(snapshot.moveEvaluationCount()), "Accepted",
                snapshot.acceptedPercentText(), contentWidth));
        body.add(twoColumns("Score calc", format(snapshot.scoreCalculationCount()), "Calc speed",
                format(snapshot.scoreCalculationSpeed()) + "/s", contentWidth));

        // Borders, the problem size line and body always fit; phase history gets whatever is left
        // of the height budget, dropping the oldest lines first.
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

    private static String problemSizeLine(DashboardSnapshot snapshot, int contentWidth) {
        var text = snapshot.approximateProblemScaleText() == null
                ? "Problem  Waiting to start..." // mirrors the phaseLine placeholder wording above
                : "Problem  %s entities, %s variables, %s values, scale %s".formatted(
                        format(snapshot.entityCount()), format(snapshot.variableCount()),
                        format(snapshot.approximateValueCount()), snapshot.approximateProblemScaleText());
        return content(text, "", contentWidth);
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
     * Compresses {@code values} down to at most {@code targetSize} entries by averaging
     * contiguous, fixed-width groups from the front (the last group may be smaller). Returns
     * {@code values} unchanged if it already fits. Shared between rendering (compress to the
     * current terminal width) and {@link SolverDashboard}'s history bookkeeping (compress to stay
     * within its retention cap while still spanning the whole run).
     * <p>
     * Deliberately NOT a proportional "size/targetSize" split, whose every group boundary shifts
     * as {@code values} grows — reslicing already-settled history on every call even though its
     * data never changed, which visibly jumps around on a live-updating chart. A fixed group width
     * only changes (growing new groups, or occasionally widening by one) at the rare moments
     * {@code values.size()} crosses a multiple of the current width; existing groups are otherwise
     * untouched by new data arriving.
     */
    static List<Double> downsample(List<Double> values, int targetSize) {
        if (values.size() <= targetSize) {
            return values;
        }
        var groupWidth = (values.size() + targetSize - 1) / targetSize; // ceil(size / targetSize)
        var downsampled = new ArrayList<Double>();
        for (var start = 0; start < values.size(); start += groupWidth) {
            var end = Math.min(start + groupWidth, values.size());
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
