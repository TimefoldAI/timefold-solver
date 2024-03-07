package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.util.MathUtils;

import org.junit.jupiter.api.Test;

class ProblemSizeStatisticsTest {

    private static ProblemSizeStatistics getProblemSizeStatistics(long scale) {
        return new ProblemSizeStatistics(0L, 0L, 0L,
                Math.log10(scale));
    }

    @Test
    void getApproximateProblemScaleLogAsFixedPointLong() {
        var statistics = getProblemSizeStatistics(100L);
        assertThat(statistics.getApproximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 100L));

        statistics = getProblemSizeStatistics(250L);
        assertThat(statistics.getApproximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 250L));
    }

    @Test
    void formatApproximateProblemScale() {
        var statistics = getProblemSizeStatistics(100L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~100");

        statistics = getProblemSizeStatistics(250L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~250");

        statistics = getProblemSizeStatistics(1_234_567L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~1,234,567");

        statistics = getProblemSizeStatistics(123_456_789L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~123,456,789");

        statistics = getProblemSizeStatistics(1_123_456_789L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~1,123,456,789");

        statistics = getProblemSizeStatistics(321_123_456_789L);
        assertThat(statistics.formatApproximateProblemScale())
                .isEqualTo("~3.211235 × 10^11");
    }
}
