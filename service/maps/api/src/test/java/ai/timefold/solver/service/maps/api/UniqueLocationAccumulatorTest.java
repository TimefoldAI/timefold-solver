package ai.timefold.solver.service.maps.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;

import org.junit.jupiter.api.Test;

public class UniqueLocationAccumulatorTest {

    @Test
    void emptyLocations() {
        UniqueLocationAccumulator locationDeduplicator = new UniqueLocationAccumulator();
        List<Location> deduplicatedLocations = locationDeduplicator.asList();
        assertThat(deduplicatedLocations).isEmpty();
    }

    @Test
    void duplicateLocationsDetected() {
        final double latitude1 = 49.288087;
        final double longitude1 = 16.562172;
        final double latitude2 = 50.288087;
        final double longitude2 = 17.562172;

        UniqueLocationAccumulator locationDeduplicator = new UniqueLocationAccumulator();
        assertThat(locationDeduplicator.addLocation(new Location(latitude1, longitude1))).isFalse();
        assertThat(locationDeduplicator.addLocation(new Location(latitude2, longitude2))).isFalse();
        // duplicates are detected
        assertThat(locationDeduplicator.addLocation(new Location(latitude1, longitude1))).isTrue();
        assertThat(locationDeduplicator.addLocation(new Location(latitude2, longitude2))).isTrue();
    }

    @Test
    void noDuplicateLocations() {
        final double latitude1 = 49.288087;
        final double longitude1 = 16.562172;
        final double latitude2 = 50.288087;
        final double longitude2 = 17.562172;

        // contains duplicates of two distinct locations
        List<Location> locations = List.of(
                new Location(latitude1, longitude1),
                new Location(latitude2, longitude2),
                new Location(latitude1, longitude1),
                new Location(latitude2, longitude2));

        LocationDeduplicator locationDeduplicator = new LocationDeduplicator();
        List<Location> deduplicatedLocations = locations.stream().map(locationDeduplicator::same).toList();

        UniqueLocationAccumulator uniqueLocationAccumulator = new UniqueLocationAccumulator();
        for (Location location : deduplicatedLocations) {
            assertThat(uniqueLocationAccumulator.addLocation(location)).isFalse();
        }
        List<Location> uniqueLocations = uniqueLocationAccumulator.asList();

        assertThat(uniqueLocations).hasSize(2);
        assertThat(uniqueLocations).containsExactly(new Location(latitude1, longitude1),
                new Location(latitude2, longitude2));
    }
}
