package ai.timefold.solver.service.maps.service.client.impl.bucketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class StaticDaypartBucketingTest {

    private final StaticDaypartBucketing bucketing = new StaticDaypartBucketing();

    @Test
    void boundariesAreHalfOpen() {
        // Night: [00:00, 06:00) and [18:00, 24:00)
        assertThat(bucketing.timeframeOf(at(0, 0))).isEqualTo(StaticDaypartBucketing.NIGHT);
        assertThat(bucketing.timeframeOf(at(5, 59))).isEqualTo(StaticDaypartBucketing.NIGHT);
        assertThat(bucketing.timeframeOf(at(18, 0))).isEqualTo(StaticDaypartBucketing.NIGHT);
        assertThat(bucketing.timeframeOf(at(23, 59))).isEqualTo(StaticDaypartBucketing.NIGHT);
        // Morning: [06:00, 12:00)
        assertThat(bucketing.timeframeOf(at(6, 0))).isEqualTo(StaticDaypartBucketing.MORNING);
        assertThat(bucketing.timeframeOf(at(11, 59))).isEqualTo(StaticDaypartBucketing.MORNING);
        // Afternoon: [12:00, 18:00)
        assertThat(bucketing.timeframeOf(at(12, 0))).isEqualTo(StaticDaypartBucketing.AFTERNOON);
        assertThat(bucketing.timeframeOf(at(17, 59))).isEqualTo(StaticDaypartBucketing.AFTERNOON);
    }

    @Test
    void bucketsAreComparedInInputsOwnOffset() {
        // 10:00 UTC = 12:00 in +02 — different buckets, because comparison uses the local hour.
        OffsetDateTime utc = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime plus2 = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2));
        assertThat(bucketing.timeframeOf(utc)).isEqualTo(StaticDaypartBucketing.MORNING);
        assertThat(bucketing.timeframeOf(plus2)).isEqualTo(StaticDaypartBucketing.AFTERNOON);
    }

    @Test
    void allTimeframesReturnsExpectedKeys() {
        assertThat(bucketing.allTimeframes()).containsExactly(
                StaticDaypartBucketing.MORNING,
                StaticDaypartBucketing.AFTERNOON,
                StaticDaypartBucketing.NIGHT);
    }

    @Test
    void indexOfMatchesAllTimeframesOrder() {
        assertThat(bucketing.indexOf(at(8, 0))).isEqualTo(0); // morning
        assertThat(bucketing.indexOf(at(14, 0))).isEqualTo(1); // afternoon
        assertThat(bucketing.indexOf(at(22, 0))).isEqualTo(2); // night
        // Invariant: allTimeframes().get(indexOf(t)) == timeframeOf(t)
        for (int h = 0; h < 24; h++) {
            OffsetDateTime t = at(h, 0);
            assertThat(bucketing.allTimeframes().get(bucketing.indexOf(t)))
                    .isEqualTo(bucketing.timeframeOf(t));
        }
    }

    @Test
    void timeframesOf_narrowIntervalWithinOneBucket() {
        // [09:00, 11:00) — entirely within MORNING
        assertThat(bucketing.timeframesOf(at(9, 0), at(11, 0)))
                .containsExactly(StaticDaypartBucketing.MORNING);
    }

    @Test
    void timeframesOf_intervalSpanningTwoBuckets() {
        // [09:00, 14:00) — spans MORNING and AFTERNOON (crosses 12:00 boundary)
        assertThat(bucketing.timeframesOf(at(9, 0), at(14, 0)))
                .containsExactlyInAnyOrder(StaticDaypartBucketing.MORNING, StaticDaypartBucketing.AFTERNOON);
    }

    @Test
    void timeframesOf_toExactlyOnBoundaryIsExclusive() {
        // [09:00, 18:00) — to=18:00 is exclusive, so NIGHT bucket is NOT included
        assertThat(bucketing.timeframesOf(at(9, 0), at(18, 0)))
                .containsExactlyInAnyOrder(StaticDaypartBucketing.MORNING, StaticDaypartBucketing.AFTERNOON);
    }

    @Test
    void timeframesOf_toJustPastBoundaryIncludesNextBucket() {
        // [09:00, 18:01) — just past 18:00, so NIGHT bucket IS included
        assertThat(bucketing.timeframesOf(at(9, 0), at(18, 1)))
                .containsExactlyInAnyOrder(
                        StaticDaypartBucketing.MORNING,
                        StaticDaypartBucketing.AFTERNOON,
                        StaticDaypartBucketing.NIGHT);
    }

    @Test
    void timeframesOf_zeroLengthIntervalCoversFromBucket() {
        // [12:00, 12:00) — zero-length, but still covers AFTERNOON (from's bucket)
        assertThat(bucketing.timeframesOf(at(12, 0), at(12, 0)))
                .containsExactly(StaticDaypartBucketing.AFTERNOON);
    }

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2024, 1, 1, hour, minute, 0, 0, ZoneOffset.UTC);
    }

}
