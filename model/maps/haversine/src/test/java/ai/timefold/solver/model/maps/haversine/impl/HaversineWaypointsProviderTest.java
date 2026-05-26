package ai.timefold.solver.model.maps.haversine.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import ai.timefold.solver.model.maps.api.model.Location;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HaversineWaypointsProviderTest {

    @Inject
    HaversineWaypointsProvider waypointsService;

    @Test
    public void getWaypoints() {
        List<Location> baseWaypointLocations = List.of(new Location(42.529529, 1.572601),
                new Location(42.516657, 1.528570),
                new Location(42.504635, 1.522390));

        Collection<Location> waypoints = waypointsService.getWaypoints(baseWaypointLocations, Collections.emptyMap());

        Assertions.assertThat(waypoints).isEqualTo(baseWaypointLocations);
    }

}
