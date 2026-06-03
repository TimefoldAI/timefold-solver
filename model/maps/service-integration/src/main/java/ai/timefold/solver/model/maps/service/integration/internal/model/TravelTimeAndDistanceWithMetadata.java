package ai.timefold.solver.model.maps.service.integration.internal.model;

import java.util.List;

public record TravelTimeAndDistanceWithMetadata(TravelTimeAndDistance travelTimeAndDistance,
        List<Integer> locationsNotInMapIdx, String resolvedMapLocation) {

    public TravelTimeAndDistanceWithMetadata(TravelTimeAndDistance travelTimeAndDistance,
            List<Integer> locationsNotInMapIdx) {
        this(travelTimeAndDistance, locationsNotInMapIdx, null);
    }
}
