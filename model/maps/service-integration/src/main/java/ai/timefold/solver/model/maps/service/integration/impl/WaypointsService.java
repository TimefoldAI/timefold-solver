package ai.timefold.solver.model.maps.service.integration.impl;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.model.maps.api.model.Waypoints;

public interface WaypointsService {

    /**
     * @param runId - unique identifier of the run the waypoints should be collected for
     * @param objectIds optional identifiers of the objects the way points should only be collected for e.g. vehicle id
     * @return A list of waypoints extracted from the user model.
     */
    List<Waypoints> getWaypoints(String runId, Set<String> objectIds);

}
