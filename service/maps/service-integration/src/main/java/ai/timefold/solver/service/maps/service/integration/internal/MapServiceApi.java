package ai.timefold.solver.service.maps.service.integration.internal;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.service.maps.api.model.Location;

public interface MapServiceApi {

    @POST
    @Path("/v1/distances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getTravelTimeAndDistance(List<Location> locations, @QueryParam("options") String options);

    @GET
    @Path("/v1/distances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getTravelTimeAndDistanceUpdates(@QueryParam("options") String options, @QueryParam("matrix-hash") String hash);

    @POST
    @Path("/v1/waypoints")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<Location> getWaypoints(List<Location> locations, @QueryParam("options") String options);

    @POST
    @Path("/v1/locations-out-of-map")
    @Consumes(MediaType.APPLICATION_JSON)
    List<Integer> getLocationsOutOfMap(List<Location> locations, @QueryParam("options") String options);

}
