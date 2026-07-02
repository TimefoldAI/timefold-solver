package ai.timefold.solver.service.maps.service.client.impl;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;

public record CacheItem(TravelTimeAndDistance travelTimeAndDistance, List<Location> locations, String hash,
        List<Integer> locationsOutOfMap, String resolvedMapLocation) {

}
