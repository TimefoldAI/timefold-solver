package ai.timefold.solver.service.maps.service.integration.internal.provider;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.maps.api.model.Location;

public interface WaypointsProvider extends ProviderIdentifier {

    List<Location> getWaypoints(List<Location> locations, Map<String, String> options);

}
