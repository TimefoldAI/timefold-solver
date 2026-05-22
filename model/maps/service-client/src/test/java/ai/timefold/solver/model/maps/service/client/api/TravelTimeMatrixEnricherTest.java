package ai.timefold.solver.model.maps.service.client.api;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.travel.TravelDistance;
import ai.timefold.solver.model.maps.api.model.travel.TravelTime;
import ai.timefold.solver.model.maps.service.client.util.RemoteMapServiceConfigurationProfile;
import ai.timefold.solver.model.maps.service.client.util.SampleModel;
import ai.timefold.solver.model.maps.service.integration.api.LocationsAwareSolverModel;
import ai.timefold.solver.model.maps.service.test.api.MapServiceApiWiremockExtensions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(MapServiceApiWiremockExtensions.class)
@TestProfile(RemoteMapServiceConfigurationProfile.class)
public class TravelTimeMatrixEnricherTest {

    @Inject
    TravelTimeMatrixEnricher enricher;

    @Test
    void testRemoteConnectionWithMapServer() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        SampleModel sampleModel = new SampleModel(List.of(l1, l2));

        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationSetName()).isEqualTo(Optional.empty());
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l2)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l2)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void testRemoteConnectionWithMapServerWithNamedLocations() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        SampleModel sampleModel = new SampleModel("name", List.of(l1, l2));

        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationSetName()).isEqualTo(Optional.of("name"));
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l2)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l2)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void testRemoteConnectionWithMapServerWithUnknownNamedLocation() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);

        SampleModel sampleModel = new SampleModel("unknown", List.of(l1, l2));

        // Pre-populate cache
        enricher.enrich(sampleModel);
        // Request for update will fail the first time because it simulates that the "unknown" location set was deleted
        // but then requests full distance matrix again
        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationSetName()).isEqualTo(Optional.of("unknown"));
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l2)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l2)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void testDistanceMatrixWithUpdates() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        Location l3 = new Location(2, 2);
        Location l4 = new Location(3, 3);

        SampleModel sampleModel = new SampleModel("name2", List.of(l1, l2, l3, l4));

        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationSetName()).isEqualTo(Optional.of("name2"));
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l2)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l2)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l3)).isEqualTo(TravelTime.of(22642L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l3)).isEqualTo(TravelDistance.of(314475L));
        Assertions.assertThat(enrich.getLocations().get(1).getTravelTimeTo(l1)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().get(1).getDistanceTo(l1)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocations().get(2).getDistanceTo(l4)).isEqualTo(TravelDistance.of(157178L));
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void testOneLocationShouldReturnEmptyDistanceMatrix() {
        Location l1 = new Location(0, 0);

        SampleModel sampleModel = new SampleModel(List.of(l1));

        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationSetName()).isEqualTo(Optional.empty());
        Assertions.assertThat(enrich.getLocations().size()).isEqualTo(1);
        Assertions.assertThat(enrich.getLocations().getFirst()).isEqualTo(l1);
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l1)).isEqualTo(TravelTime.ZERO);
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l1)).isEqualTo(TravelDistance.ZERO);
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();
    }

    @Test
    void distanceMatrixWithUpdateToClientCacheRetrievesFromCache() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        List<Location> allLocations = List.of(l1, l2);

        SampleModel sampleModel = new SampleModel("name", allLocations);
        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);
        LocationsAwareSolverModel<?> enrich2 = enricher.enrich(sampleModel);
        Assertions.assertThat(enrich.getLocationsNotInMap()).isEmpty();

        Assertions.assertThat(enrich).isEqualTo(enrich2);
        for (Location location : allLocations) {
            Assertions.assertThat(enrich2.getLocations().getFirst().getTravelTimeTo(location)).isNotEqualTo(TravelTime.of(-1));
        }
    }

    @Test
    void testDistanceMatrixWithLocationsOutOfMap() {
        Location l1 = new Location(0, 0);
        Location l2 = new Location(1, 1);
        Location l3 = new Location(2, 2);
        Location l4 = new Location(-90, -90);

        SampleModel sampleModel = new SampleModel(List.of(l1, l2, l3, l4));

        LocationsAwareSolverModel<?> enrich = enricher.enrich(sampleModel);

        Assertions.assertThat(enrich.getLocationsNotInMap()).contains(l4);
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l2)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l2)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocations().getFirst().getTravelTimeTo(l3)).isEqualTo(TravelTime.of(22642L));
        Assertions.assertThat(enrich.getLocations().getFirst().getDistanceTo(l3)).isEqualTo(TravelDistance.of(314475L));
        Assertions.assertThat(enrich.getLocations().get(1).getTravelTimeTo(l1)).isEqualTo(TravelTime.of(11322L));
        Assertions.assertThat(enrich.getLocations().get(1).getDistanceTo(l1)).isEqualTo(TravelDistance.of(157249L));
        Assertions.assertThat(enrich.getLocations().get(2).getDistanceTo(l4)).isEqualTo(TravelDistance.ZERO);
    }

}
