package ai.timefold.solver.console.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardRendererTest {

    private static DashboardSnapshot snapshot(String acceptedPercentText, List<Long> history, List<String> phaseLines) {
        return new DashboardSnapshot(
                83_000L, "Local Search", 4811L, "-12hard/-340soft", "-12hard/-352soft", acceptedPercentText,
                9210L, 1_200_000L, 1_400_000L, 300L, history, phaseLines);
    }

    @Test
    void allLinesSameWidth() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(10L, 20L, 5L), List.of()), 60, 20);
        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(60));
    }

    @Test
    void boxHasCorners() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(10L), List.of()), 60, 20);
        assertThat(lines.getFirst()).startsWith("┌").endsWith("┐");
        assertThat(lines.getLast()).startsWith("└").endsWith("┘");
    }

    @Test
    void missingAcceptedRateShowsDash() {
        var lines = DashboardRenderer.render(snapshot("—", List.of(), List.of()), 60, 20);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("—"));
    }

    @Test
    void adaptsToTerminalWidth() {
        var wide = DashboardRenderer.render(snapshot("38.1 %", List.of(1L, 2L), List.of()), 80, 20);
        var narrow = DashboardRenderer.render(snapshot("38.1 %", List.of(1L, 2L), List.of()), 40, 20);
        assertThat(wide.getFirst()).hasSize(80);
        assertThat(narrow.getFirst()).hasSize(40);
    }

    @Test
    void respectsHeightBudgetForPhaseHistory() {
        var manyLines = List.of("phase 1 ended", "phase 2 ended", "phase 3 ended", "phase 4 ended", "phase 5 ended");
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1L, 2L), manyLines), 60, 12);
        assertThat(lines.size()).isLessThanOrEqualTo(12);
    }

    @Test
    void capsWidthAt120AndCentersOnWideTerminals() {
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1L, 2L), List.of()), 200, 20);
        var expectedLeftPad = " ".repeat((200 - 120) / 2);
        assertThat(lines).allSatisfy(line -> assertThat(line).hasSize(120 + expectedLeftPad.length()));
        assertThat(lines.getFirst()).startsWith(expectedLeftPad + "┌");
    }

    @Test
    void showsAllPhaseHistoryWhenItFits() {
        var oneLine = List.of("0:04 Construction Heuristic ended: 300 steps, 750/s");
        var lines = DashboardRenderer.render(snapshot("38.1 %", List.of(1L, 2L), oneLine), 60, 30);
        assertThat(lines).anySatisfy(line -> assertThat(line).contains("Construction Heuristic ended"));
    }

}
