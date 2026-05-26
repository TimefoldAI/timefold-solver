package ai.timefold.solver.model.maps.service.integration.internal.provider;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.model.maps.api.model.Location;

public interface WaypointsProvider extends ProviderIdentifier {

    List<Location> getWaypoints(List<Location> locations, Map<String, String> options);

}
