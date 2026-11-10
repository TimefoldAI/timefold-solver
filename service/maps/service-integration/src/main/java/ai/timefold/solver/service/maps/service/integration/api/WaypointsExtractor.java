package ai.timefold.solver.service.maps.service.integration.api;

import java.util.List;

import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.maps.api.model.Waypoints;

public non-sealed interface WaypointsExtractor<SolverModel_ extends SolverModel<?>> extends WaypointsExtractorBase {

    /**
     * Extract base waypoints from the solver model. They represent stops on a path.
     * <p>
     * <br>
     * Example:
     * <ul>
     * <li>Vehicle has start location A and end location E.</li>
     * <li>Vehicle visits customers at locations B, C, D.</li>
     * <li>Base waypoints are A, B, C, D, E.</li>
     * </ul>
     *
     * Base waypoints can be used to calculate enhanced route overview by inserting intermediate waypoints that provide finer
     * routing accuracy
     * (e.g. actual turns)
     * <p>
     * <br>
     * Example:
     * <ul>
     * <li>Base waypoints are A, B, C, D, E.</li>
     * <li>Enhanced waypoints are A, AB1, AB2, B, BC1, BC2, C, CD1, D, E.</li>
     * </ul>
     *
     * @param solverModel Solver model to extract waypoints from.
     * @return A list of waypoints extracted from the solver model.
     */
    List<Waypoints> extractBaseWaypoints(SolverModel_ solverModel);
}
