package ai.timefold.solver.service.maps.service.integration.internal.provider;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.maps.api.model.Location;

public interface TravelTimeAndDistanceMatrixProvider extends ProviderIdentifier {

    TravelTimeAndDistanceMatrixResponse calculateTravelTimeAndDistance(List<Location> locationsSource,
            List<Location> locationsDestination,
            Map<String, String> options);

    TravelTimeAndDistanceMatrixResponse calculateTravelTimeAndDistance(List<Location> locations, Map<String, String> options);

    List<Integer> getLocationsOutOfMap(List<Location> locations, Map<String, String> options);

}
