package ai.timefold.solver.service.maps.service.integration.api;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TransportType;

public interface LocationsAwareSolverModel<Score_ extends Score<Score_>> extends SolverModel<Score_> {

    List<Location> getLocations();

    default List<TransportType> getTransportTypes() {
        return List.of();
    }

    Optional<String> getLocationSetName();

    void setLocationsNotInMap(List<Location> locationsNotInMap);

    List<Location> getLocationsNotInMap();

}
