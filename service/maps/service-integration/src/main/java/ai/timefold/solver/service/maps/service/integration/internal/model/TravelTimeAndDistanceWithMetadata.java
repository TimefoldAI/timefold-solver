package ai.timefold.solver.service.maps.service.integration.internal.model;

import java.util.List;

public record TravelTimeAndDistanceWithMetadata(TravelTimeAndDistance travelTimeAndDistance,
        List<Integer> locationsNotInMapIdx) {
}
