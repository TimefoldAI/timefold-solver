package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Locale;

import ai.timefold.solver.core.impl.util.MathUtils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProblemSizeStatisticsTest {

    private static ProblemSizeStatistics getProblemSizeStatisticsFromCountLong(long scale) {
        return new ProblemSizeStatistics(0L, Collections.emptySortedMap(), 0L, 0L,
                Collections.emptySortedMap(),
                Math.log10(scale));
    }

    private static ProblemSizeStatistics getProblemSizeStatisticsFromDoubleLog(double scale) {
        return new ProblemSizeStatistics(0L, Collections.emptySortedMap(), 0L, 0L,
                Collections.emptySortedMap(),
                scale);
    }

    private static Locale defaultLocaleToRestore;

    @BeforeAll
    public static void setLocale() {
        defaultLocaleToRestore = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(defaultLocaleToRestore);
        defaultLocaleToRestore = null;
    }

    @Test
    void getApproximateProblemScaleLogAsFixedPointLong() {
        var statistics = getProblemSizeStatisticsFromCountLong(100L);
        assertThat(statistics.approximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 100L));

        statistics = getProblemSizeStatisticsFromCountLong(250L);
        assertThat(statistics.approximateProblemScaleLogAsFixedPointLong())
                .isEqualTo(MathUtils.getScaledApproximateLog(MathUtils.LOG_PRECISION, 10L, 250L));
    }

    @Test
    void formatApproximateProblemScale() {
        var statistics = getProblemSizeStatisticsFromCountLong(100L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("100");

        statistics = getProblemSizeStatisticsFromCountLong(250L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("250");

        statistics = getProblemSizeStatisticsFromCountLong(1_234_567L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1,234,567");

        statistics = getProblemSizeStatisticsFromCountLong(123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("123,456,789");

        statistics = getProblemSizeStatisticsFromCountLong(1_123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1,123,456,789");

        statistics = getProblemSizeStatisticsFromCountLong(321_123_456_789L);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("3.211235 × 10^11");

        // scale = -infinity
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.NEGATIVE_INFINITY);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = +infinity
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.POSITIVE_INFINITY);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = NaN
        statistics = getProblemSizeStatisticsFromDoubleLog(Double.NaN);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("0");

        // scale = 0
        statistics = getProblemSizeStatisticsFromDoubleLog(0);
        assertThat(statistics.approximateProblemScaleAsFormattedString())
                .isEqualTo("1");
    }
}
