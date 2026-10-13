package ai.timefold.solver.service.maps.service.integration.api;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.maps.api.model.Location;

public interface LocationsAwareSolverModel<Score_ extends Score<Score_>> extends SolverModel<Score_> {

    List<Location> getLocations();

    /**
     * Returns the optional name of the location set used for map-service requests.
     *
     * @return the location-set name, or {@link Optional#empty()} when no name is configured
     */
    default Optional<String> getLocationSetName() {
        return Optional.empty();
    }

    void setLocationsNotInMap(List<Location> locationsNotInMap);

    List<Location> getLocationsNotInMap();

}
