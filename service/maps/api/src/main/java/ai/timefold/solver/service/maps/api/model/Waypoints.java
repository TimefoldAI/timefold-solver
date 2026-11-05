package ai.timefold.solver.service.maps.api.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Schema(name = "Waypoints", description = "The road geometry of one route, from the map service.")
public record Waypoints(
        @Schema(description = "ID of the route.") String id,
        @Schema(description = "Points along the roads of the route, in driving order.") List<Location> waypoints,
        @JsonIgnore boolean calculated) {

    public Waypoints(String id, List<Location> waypoints) {
        this(id, waypoints, false);
    }

}
