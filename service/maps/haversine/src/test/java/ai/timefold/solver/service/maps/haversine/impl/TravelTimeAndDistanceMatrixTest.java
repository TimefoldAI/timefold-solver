package ai.timefold.solver.service.maps.haversine.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TravelTimeAndDistanceMatrixTest {

    @Inject
    ObjectMapper mapper;

    @Inject
    HaversineTravelTimeAndDistanceMatrixProvider provider;

    @Test
    void matrixDeserializationShouldBeSuccessful() throws IOException {

        Location locationOne = new Location(33.700770256281146, -84.32057487796858);
        Location locationTwo = new Location(33.69706463808921, -84.3949320945087);
        Location locationThree = new Location(33.75, -84.30);

        TravelTimeAndDistance travelTimeAndDistance = provider.calculateBulkDistance(
                List.of(locationOne, locationTwo, locationThree), List.of(locationOne, locationTwo, locationThree));

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mapper.writeValueAsBytes(travelTimeAndDistance));
        TravelTimeAndDistance travelTimeAndDistanceResult = mapper.readValue(byteArrayInputStream, TravelTimeAndDistance.class);

        assertThat(travelTimeAndDistanceResult.distance().get(locationOne, locationThree)).isEqualTo(5795);
        assertThat(travelTimeAndDistanceResult.distance().get(locationTwo, locationThree)).isEqualTo(10570);
        assertThat(travelTimeAndDistanceResult.distance().get(locationOne, locationTwo)).isEqualTo(6891);
    }
}
