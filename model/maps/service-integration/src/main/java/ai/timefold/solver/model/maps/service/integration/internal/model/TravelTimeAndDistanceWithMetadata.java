package ai.timefold.solver.model.maps.service.integration.internal.model;

import java.util.List;

public record TravelTimeAndDistanceWithMetadata(TravelTimeAndDistance travelTimeAndDistance,
        List<Integer> locationsNotInMapIdx) {
}
