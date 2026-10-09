package ai.timefold.solver.console.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardRendererTest {

    private static final String IDENTIFICATION = "Timefold Solver Community Edition v1.2.3";

    // Wraps history as a single, unlabelled score level (the SimpleScore case); enough unless a test is about per-level rows.
    private static DashboardSnapshot snapshot(String acceptedPercentText, List<Double> history, List<String> phaseLines) {
        return new DashboardSnapshot(
                83_000L, "Local Search", 4811L, "-12hard/-340soft", "-12hard/-352soft", acceptedPercentText,
                9210L, 1_200_000L, 300L, 3L, 1_000_000L, "2.8 × 10^41",
                history.isEmpty() ? List.of() : List.of("score"),
                history.isEmpty() ? List.of() : List.of(history),
                phaseLines);
    }

    private static FinalSummary finalSummary(String title) {
        return new FinalSummary(title, "0hard/15soft", 104_864L, 84_760L, 104_864L, 456_000L, 12_456L, "11.9 %", 104_864L);
    }

    @Test
    void allLinesSameWidth() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(10.0, 20.0, 5.0), List.of()), 60, 20, IDENTIFICATION);
        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(60));
    }

    @Test
    void boxHasCorners() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(10.0), List.of()), 60, 20, IDENTIFICATION);
        assertThat(lines.getFirst()).startsWith("┌").endsWith("┐");
        assertThat(lines.getLast()).startsWith("└").endsWith("┘");
    }

    @Test
    void missingAcceptedRateShowsDash() {
        var lines = DashboardRenderer.render(snapshot("—", List.of(), List.of()), 60, 20, IDENTIFICATION);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("—"));
    }

    @Test
    void adaptsToTerminalWidth() {
        var wide = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), List.of()), 80, 20, IDENTIFICATION);
        var narrow = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), List.of()), 40, 20, IDENTIFICATION);
        assertThat(wide.getFirst()).hasSize(80);
        assertThat(narrow.getFirst()).hasSize(40);
    }

    @Test
    void respectsHeightBudgetForPhaseHistory() {
        var manyLines = List.of("phase 1 ended", "phase 2 ended", "phase 3 ended", "phase 4 ended", "phase 5 ended");
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), manyLines), 60, 12, IDENTIFICATION);
        assertThat(lines.size()).isLessThanOrEqualTo(12);
    }

    @Test
    void capsWidthAt120AndCentersOnWideTerminals() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), List.of()), 200, 20, IDENTIFICATION);
        var expectedLeftPad = " ".repeat((200 - 120) / 2);
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(120 + expectedLeftPad.length()));
        assertThat(lines.getFirst()).startsWith(expectedLeftPad + "┌");
    }

    @Test
    void twoColumnStatsAlignAcrossRowsRegardlessOfValueLength() {
        // Fixture values deliberately differ in length (e.g. "4,812" vs "1,200,000"); used to shift the second column.
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0), List.of()), 120, 20, IDENTIFICATION);
        var stepsLine = lines.stream().filter(line -> line.contains("Steps:")).findFirst().orElseThrow();
        var movesEvalLine = lines.stream().filter(line -> line.contains("Moves eval:")).findFirst().orElseThrow();
        assertThat(stepsLine.indexOf("Speed:")).isEqualTo(movesEvalLine.indexOf("Accepted:"));
    }

    @Test
    void showsAllPhaseHistoryWhenItFits() {
        var oneLine = List.of("0:04 Construction Heuristic ended: 300 steps, 750/s");
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), oneLine), 60, 30, IDENTIFICATION);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("Construction Heuristic ended"));
    }

    @Test
    void titleShowsFullIdentificationWhenItFits() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0), List.of()), 120, 20, IDENTIFICATION);
        assertThat(lines.getFirst()).contains(IDENTIFICATION);
    }

    @Test
    void titleTruncatesButNeverBelowTimefold() {
        var longIdentification = "Timefold Solver Enterprise Edition v1.2.3 (core a1b2c3d, enterprise e4f5g6h)";
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0), List.of()), 40, 20, longIdentification);
        var topBorderLine = lines.getFirst();
        assertThat(topBorderLine).doesNotContain(longIdentification);
        assertThat(topBorderLine).contains("Timefold");
        assertThat(topBorderLine).hasSize(40);
    }

    @Test
    void sparklineLineFitsWithoutTruncatingTheLabel() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 5.0, 3.0), List.of()), 60, 20, IDENTIFICATION);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("best score over time"));
    }

    @Test
    void downsampleReturnsInputUnchangedWhenItAlreadyFits() {
        var values = List.of(1.0, 2.0, 3.0);
        assertThat(DashboardRenderer.downsample(values, 5)).isEqualTo(values);
    }

    @Test
    void downsampleAveragesContiguousGroupsWhenTooLong() {
        var values = List.of(0.0, 0.0, 10.0, 10.0);
        assertThat(DashboardRenderer.downsample(values, 2)).containsExactly(0.0, 10.0);
    }

    @Test
    void downsampleNeverProducesFewerThanTheFullTargetWidth() {
        // Regression: an integer group width (ceil(size/target)) jumps from 1 to 2 the moment size first exceeds
        // target; proportional slicing always produces exactly `target` buckets instead.
        var target = 20;
        for (var size = 1; size <= 50; size++) {
            var values = new ArrayList<Double>();
            for (var i = 0; i < size; i++) {
                values.add((double) i);
            }
            assertThat(DashboardRenderer.downsample(values, target)).hasSize(Math.min(size, target));
        }
    }

    @Test
    void sparklineSpansTheWholeRunNotJustTheRecentTail() {
        var history = new ArrayList<Double>();
        for (var i = 0; i < 50; i++) {
            history.add(1.0);
        }
        for (var i = 0; i < 50; i++) {
            history.add(100.0);
        }
        var lines = DashboardRenderer.render(snapshot("38.1 %", history, List.of()), 40, 20, IDENTIFICATION);
        var sparklineLine = lines.stream().filter(line -> line.contains("score  ")).findFirst().orElseThrow();
        var barsStart = sparklineLine.indexOf("score  ") + "score  ".length();
        var sparklineChars = sparklineLine.substring(barsStart, sparklineLine.length() - 2); // trims the " │" border
        // Strips trailing blank padding columns before comparing real data.
        var realBars = sparklineChars.stripTrailing();
        // A tail-only view would show only the flat "100.0" half; spanning the whole run makes the transition visible.
        assertThat(realBars.charAt(0)).isNotEqualTo(realBars.charAt(realBars.length() - 1));
    }

    @Test
    void sparklineIsNotFlattenedByAnOldFarAwaySpike() {
        var history = new ArrayList<Double>();
        // Bucket 0: an extreme early spike diluted by averaging with 9 normal-range values.
        history.add(10_000.0);
        for (var i = 0; i < 9; i++) {
            history.add(500.0);
        }
        // Remaining 13 buckets: a real, later, small-scale ramp in a much narrower range.
        for (var bucket = 0; bucket < 13; bucket++) {
            var value = 500.0 + bucket * 50.0;
            for (var i = 0; i < 10; i++) {
                history.add(value);
            }
        }
        var lines = DashboardRenderer.render(snapshot("38.1 %", history, List.of()), 40, 20, IDENTIFICATION);
        var sparklineLine = lines.stream().filter(line -> line.contains("score  ")).findFirst().orElseThrow();
        var barsStart = sparklineLine.indexOf("score  ") + "score  ".length();
        var sparklineChars = sparklineLine.substring(barsStart, sparklineLine.length() - 2); // trims the " │" border
        // Normalizing against raw min/max (10_000 vs 500) would flatten this 500-1100 ramp; against displayed values doesn't.
        assertThat(sparklineChars.chars().distinct().count()).isGreaterThan(1);
    }

    @Test
    void repeatedHistoryCompactionSpreadsNewDataAcrossManyBucketsNotJustTheLast() {
        // Mirrors SolverDashboard's own compaction: grow past 2x the limit, then halve in one clean pass, rather
        // than downsampling by 1 every step (which sticks the newest bucket at a stale average forever).
        var limit = 200;
        var history = new ArrayDeque<Double>();
        for (var i = 0; i < limit; i++) {
            history.addLast(1.0);
        }
        for (var i = 0; i < limit; i++) {
            history.addLast(2.0);
            if (history.size() >= 2 * limit) {
                var halved = DashboardRenderer.downsample(List.copyOf(history), limit);
                history.clear();
                history.addAll(halved);
            }
        }
        var bucketsReflectingTheNewerData = history.stream().filter(value -> value > 1.5).count();
        assertThat(bucketsReflectingTheNewerData).isGreaterThan(50);
    }

    @Test
    void problemSizeLineIsFirstContentLineBeforePhaseHistory() {
        var oneLine = List.of("0:04 Construction Heuristic ended: 300 steps, 750/s");
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1.0, 2.0), oneLine), 120, 30, IDENTIFICATION);
        assertThat(lines.get(0)).startsWith("┌");
        assertThat(lines.get(1)).contains("300 entities", "3 variables", "1,000,000 values", "2.8 × 10^41");
        assertThat(lines.get(2)).contains("Construction Heuristic ended");
    }

    @Test
    void problemSizeLineShowsPlaceholderBeforeItIsKnown() {
        var snapshotBeforeSolvingStarted = new DashboardSnapshot(
                0L, null, -1L, "n/a", "n/a", "—",
                0L, 0L, 0L, 0L, 0L, null, List.of(), List.of(), List.of());
        var lines = DashboardRenderer.render(snapshotBeforeSolvingStarted, 60, 20, IDENTIFICATION);
        assertThat(lines.get(1)).contains("Waiting to start...");
    }

    @Test
    void oneSparklineRowPerScoreLevel() {
        var snapshot = new DashboardSnapshot(
                83_000L, "Local Search", 4811L, "-12hard/-340soft", "-12hard/-352soft", "38.1 %",
                9210L, 1_200_000L, 300L, 3L, 1_000_000L, "2.8 × 10^41",
                List.of("hard", "soft"), List.of(List.of(1.0, 2.0, 3.0), List.of(10.0, 5.0, 1.0)), List.of());
        var lines = DashboardRenderer.render(snapshot, 60, 20, IDENTIFICATION);
        var hardLine = lines.stream().filter(line -> line.contains("hard  ")).findFirst().orElseThrow();
        var softLine = lines.stream().filter(line -> line.contains("soft  ")).findFirst().orElseThrow();
        // A hard-constraint repair paid for with a soft-score cost must not read as one collapsing line.
        assertThat(hardLine).isNotEqualTo(softLine);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("best score over time"));
    }

    @Test
    void narrowTerminalStacksStatRowsInsteadOfTruncatingValues() {
        // At MIN_WIDTH (36 content columns), these values need 45 combined; stacking into 4 rows must not truncate any.
        var snapshot = new DashboardSnapshot(
                83_000L, "Local Search", 5_234_788L, "-12hard/-340soft", "-12hard/-352soft", "38.1 %",
                812_345L, 1_200_000_000L, 300L, 3L, 1_000_000L, "2.8 × 10^41",
                List.of(), List.of(), List.of());
        var lines = DashboardRenderer.render(snapshot, 40, 20, IDENTIFICATION);
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(40));
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("5,234,789"));
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("1,200,000,000"));
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("812,345/s"));
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("38.1 %"));
    }

    @Test
    void finalSummaryHasDoubleLineCorners() {
        var lines = DashboardRenderer.renderFinalSummary(finalSummary("FEASIBLE SOLUTION FOUND"), 80);
        assertThat(lines.getFirst()).startsWith("╔").endsWith("╗");
        assertThat(lines.getLast()).startsWith("╚").endsWith("╝");
    }

    @Test
    void finalSummaryTitleIsCentered() {
        var title = "FEASIBLE SOLUTION FOUND";
        var lines = DashboardRenderer.renderFinalSummary(finalSummary(title), 80);
        var titleLine = lines.get(1);
        assertThat(titleLine).contains(title);
        var titleStart = titleLine.indexOf(title);
        var titleEnd = titleStart + title.length();
        var leadingSpaces = titleStart - 2; // after the "║ " border prefix
        var trailingSpaces = titleLine.length() - 2 - titleEnd; // before the " ║" border suffix
        assertThat(Math.abs(leadingSpaces - trailingSpaces)).isLessThanOrEqualTo(1);
    }

    @Test
    void finalSummaryShowsAllStatsAndOmitsUnavailableOnes() {
        var lines = DashboardRenderer.renderFinalSummary(finalSummary("FEASIBLE SOLUTION FOUND"), 80);
        var text = String.join("\n", lines);
        assertThat(text).contains("Final Score:", "0hard/15soft");
        assertThat(text).contains("Steps:", "104,864");
        assertThat(text).contains("Solve Time:");
        assertThat(text).contains("Moves Evaluated:");
        assertThat(text).contains("Moves/s:", "456,000");
        assertThat(text).contains("Moves Accepted:", "12,456");
        assertThat(text).contains("Acceptance:", "11.9 %");
        assertThat(text).contains("Score Calcs:");
        // No "moves generated"/"generation time" split: not a real metric in the solver, so not fabricated here.
        assertThat(text).doesNotContainIgnoringCase("generated");
        assertThat(text).doesNotContainIgnoringCase("generation time");
        assertThat(text).doesNotContainIgnoringCase("evaluation time");
    }

    @Test
    void finalSummaryCapsWidthAt120AndCentersOnWideTerminals() {
        var lines = DashboardRenderer.renderFinalSummary(finalSummary("FEASIBLE SOLUTION FOUND"), 200);
        var expectedLeftPad = (200 - 120) / 2;
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(120 + expectedLeftPad));
    }

}
