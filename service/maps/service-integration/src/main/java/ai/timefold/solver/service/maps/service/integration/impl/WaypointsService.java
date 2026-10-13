package ai.timefold.solver.service.maps.service.integration.impl;

import java.util.List;
import java.util.Set;

import ai.timefold.solver.service.maps.api.model.Waypoints;

public interface WaypointsService {

    /**
     * @param runId - unique identifier of the run the waypoints should be collected for
     * @param routeIdSet the ids of the routes to collect the waypoints for;
     *        empty collects every route
     * @return A list of waypoints extracted from the user model.
     */
    List<Waypoints> getWaypoints(String runId, Set<String> routeIdSet);

    /**
     * Releases the in-memory waypoints of a run that finished, after its waypoints are stored or after it failed.
     * Later solution events for that id are ignored until the run starts again.
     *
     * @param runId the id of the finished run
     */
    void releaseFinishedRun(String runId);

}
