package ai.timefold.solver.model.maps.service.integration.api;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.maps.api.model.Location;

public interface LocationsAwareSolverModel<Score_ extends Score<Score_>> extends SolverModel<Score_> {

    List<Location> getLocations();

    Optional<String> getLocationSetName();

    void setLocationsNotInMap(List<Location> locationsNotInMap);

    List<Location> getLocationsNotInMap();

}
