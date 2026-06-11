package ai.timefold.solver.service.maps.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Waypoints(String id, List<Location> waypoints, @JsonIgnore boolean calculated) {

    public Waypoints(String id, List<Location> waypoints) {
        this(id, waypoints, false);
    }

}
