package ai.timefold.solver.service.maps.service.client.impl.bucketing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default v1 bucketing: splits the day into morning (06:00 inclusive – 12:00 exclusive), afternoon (12:00 – 18:00) and
 * night (18:00 – 06:00 the next day). Comparison is done in each input's own offset, so callers control the time zone
 * by choosing the offset of the {@link OffsetDateTime} they pass in.
 * <p>
 * The ordering returned by {@link #allTimeframes()} — morning (0), afternoon (1), night (2) — is the stable convention
 * the maps service client uses for its per-timeframe arrays.
 */
@ApplicationScoped
public class StaticDaypartBucketing implements TimeframeBucketing {

    public static final Timeframe MORNING = new Timeframe("morning");
    public static final Timeframe AFTERNOON = new Timeframe("afternoon");
    public static final Timeframe NIGHT = new Timeframe("night");

    private static final int MORNING_INDEX = 0;
    private static final int AFTERNOON_INDEX = 1;
    private static final int NIGHT_INDEX = 2;

    private static final List<Timeframe> ALL = List.of(MORNING, AFTERNOON, NIGHT);
    private static final int[] BUCKET_HOURS = { 6, 12, 18 };

    @Override
    public Timeframe timeframeOf(OffsetDateTime time) {
        return ALL.get(indexOf(time));
    }

    @Override
    public int indexOf(OffsetDateTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 12) {
            return MORNING_INDEX;
        } else if (hour >= 12 && hour < 18) {
            return AFTERNOON_INDEX;
        } else {
            return NIGHT_INDEX;
        }
    }

    @Override
    public List<Timeframe> allTimeframes() {
        return ALL;
    }

    @Override
    public Timeframe defaultTimeframe() {
        return MORNING;
    }

    @Override
    public Set<Timeframe> timeframesOf(OffsetDateTime from, OffsetDateTime to) {
        Set<Timeframe> result = new LinkedHashSet<>();
        result.add(timeframeOf(from));
        OffsetDateTime boundary = nextBucketBoundaryAfter(from);
        while (boundary.isBefore(to)) {
            result.add(timeframeOf(boundary));
            boundary = nextBucketBoundaryAfter(boundary);
        }
        return result;
    }

    // Returns the first bucket boundary (06:00, 12:00, or 18:00 on the same date, or 06:00 next day)
    // that is strictly after t, using t's own offset.
    private OffsetDateTime nextBucketBoundaryAfter(OffsetDateTime t) {
        LocalDate date = t.toLocalDate();
        ZoneOffset offset = t.getOffset();
        for (int bucketHours : BUCKET_HOURS) {
            OffsetDateTime candidate = OffsetDateTime.of(date, LocalTime.of(bucketHours, 0), offset);
            if (candidate.isAfter(t)) {
                return candidate;
            }
        }
        return OffsetDateTime.of(date.plusDays(1), LocalTime.of(6, 0), offset);
    }

}
