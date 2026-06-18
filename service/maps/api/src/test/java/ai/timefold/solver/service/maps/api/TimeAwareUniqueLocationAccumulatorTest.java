package ai.timefold.solver.service.maps.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;

import org.junit.jupiter.api.Test;

public class TimeAwareUniqueLocationAccumulatorTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T1 = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T2 = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T3 = OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC);

    private static final double LATITUDE_1 = 49.288087;
    private static final double LONGITUDE_1 = 16.562172;
    private static final double LATITUDE_2 = 50.288087;
    private static final double LONGITUDE_2 = 17.562172;

    @Test
    void emptyAccumulator() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        assertThat(accumulator.getLocationsWithAvailability()).isEmpty();
    }

    @Test
    void singleLocationStoredWithTimeIntervals() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        Location location = new Location(LATITUDE_1, LONGITUDE_1);
        TimeInterval interval = new TimeInterval(T0, T1);

        boolean isDuplicate = accumulator.addLocation(location, List.of(interval));

        assertThat(isDuplicate).isFalse();
        Map<Location, List<TimeInterval>> result = accumulator.getLocationsWithAvailability();
        assertThat(result).hasSize(1);
        assertThat(result.get(location)).containsExactly(interval);
    }

    @Test
    void duplicateEqualInstanceDetected() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        TimeInterval interval1 = new TimeInterval(T0, T1);
        TimeInterval interval2 = new TimeInterval(T2, T3);

        // First add — not a duplicate
        assertThat(accumulator.addLocation(new Location(LATITUDE_1, LONGITUDE_1), List.of(interval1))).isFalse();
        // Second add with a different instance having equal coordinates — duplicate detected
        assertThat(accumulator.addLocation(new Location(LATITUDE_1, LONGITUDE_1), List.of(interval2))).isTrue();
    }

    @Test
    void sameInstanceNotDetectedAsDuplicate() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        Location location = new Location(LATITUDE_1, LONGITUDE_1);
        TimeInterval interval1 = new TimeInterval(T0, T1);
        TimeInterval interval2 = new TimeInterval(T2, T3);

        assertThat(accumulator.addLocation(location, List.of(interval1))).isFalse();
        // Same instance added again — identity set already contains it, so not a duplicate
        assertThat(accumulator.addLocation(location, List.of(interval2))).isFalse();
    }

    @Test
    void timeIntervalsAccumulatedAcrossDuplicateInstances() {
        LocationDeduplicator deduplicator = new LocationDeduplicator();
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();

        Location canonical = deduplicator.same(new Location(LATITUDE_1, LONGITUDE_1));
        TimeInterval interval1 = new TimeInterval(T0, T1);
        TimeInterval interval2 = new TimeInterval(T2, T3);

        accumulator.addLocation(canonical, List.of(interval1));
        accumulator.addLocation(canonical, List.of(interval2));

        List<TimeInterval> intervals = accumulator.getLocationsWithAvailability().get(canonical);
        assertThat(intervals).containsExactly(interval1, interval2);
    }

    @Test
    void emptyTimeIntervalsAccepted() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        Location location = new Location(LATITUDE_1, LONGITUDE_1);

        boolean isDuplicate = accumulator.addLocation(location, List.of());

        assertThat(isDuplicate).isFalse();
        assertThat(accumulator.getLocationsWithAvailability().get(location)).isEmpty();
    }

    @Test
    void multipleLocationsStoredInInsertionOrder() {
        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        Location location1 = new Location(LATITUDE_1, LONGITUDE_1);
        Location location2 = new Location(LATITUDE_2, LONGITUDE_2);
        TimeInterval interval1 = new TimeInterval(T0, T1);
        TimeInterval interval2 = new TimeInterval(T2, T3);

        accumulator.addLocation(location1, List.of(interval1));
        accumulator.addLocation(location2, List.of(interval2));

        Map<Location, List<TimeInterval>> result = accumulator.getLocationsWithAvailability();
        assertThat(result).hasSize(2);
        assertThat(result.keySet()).containsExactly(location1, location2);
        assertThat(result.get(location1)).containsExactly(interval1);
        assertThat(result.get(location2)).containsExactly(interval2);
    }

    @Test
    void noDuplicatesWhenDeduplicatedFirst() {
        LocationDeduplicator deduplicator = new LocationDeduplicator();
        List<Location> raw = List.of(
                new Location(LATITUDE_1, LONGITUDE_1),
                new Location(LATITUDE_2, LONGITUDE_2),
                new Location(LATITUDE_1, LONGITUDE_1),
                new Location(LATITUDE_2, LONGITUDE_2));

        TimeAwareUniqueLocationAccumulator accumulator = new TimeAwareUniqueLocationAccumulator();
        TimeInterval interval = new TimeInterval(T0, T1);
        for (Location location : raw) {
            assertThat(accumulator.addLocation(deduplicator.same(location), List.of(interval))).isFalse();
        }

        assertThat(accumulator.getLocationsWithAvailability()).hasSize(2);
    }
}
