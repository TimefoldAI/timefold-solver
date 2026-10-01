package ai.timefold.solver.service.maps.haversine.impl;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class HaversineTravelTimeAndDistanceMatrixProviderTest {
    private final HaversineTravelTimeAndDistanceMatrixProvider provider =
            new HaversineTravelTimeAndDistanceMatrixProvider(new ObjectMapper());

    // Results have been verified with the help of https://latlongdata.com/.
    @Test
    void calculateDistance() {

        Location Gent = new Location(51.0441461, 3.7336349);
        Location Brno = new Location(49.1913945, 16.6122723);
        assertThat(provider.calculateDistance(Gent, Brno))
                .isEqualTo(939748L);

        // Close to the North Pole.
        Location Svolvaer = new Location(68.2359953, 14.5644379);
        Location Lulea = new Location(65.5887708, 22.1518707);
        assertThat(provider.calculateDistance(Svolvaer, Lulea))
                .isEqualTo(442297L);
    }

    @Test
    void calculateTravelTime() {
        Location Gent = new Location(51.0441461, 3.7336349);
        Location Brno = new Location(49.1913945, 16.6122723);
        assertThat(provider.calculateTravelTime(Gent, Brno))
                .isEqualTo(HaversineTravelTimeAndDistanceMatrixProvider.metersToDrivingSeconds(939748L));

        // Close to the North Pole.
        Location Svolvaer = new Location(68.2359953, 14.5644379);
        Location Lulea = new Location(65.5887708, 22.1518707);
        assertThat(provider.calculateTravelTime(Svolvaer, Lulea))
                .isEqualTo(HaversineTravelTimeAndDistanceMatrixProvider.metersToDrivingSeconds(442297L));
    }

    @Test
    void getLocationsOutOfMap() {
        Location Gent = new Location(51.0441461, 3.7336349);
        Location Brno = new Location(49.1913945, 16.6122723);
        assertThat(provider.getLocationsOutOfMap(List.of(Gent, Brno), emptyMap())).isEqualTo(emptyList());
    }

}
