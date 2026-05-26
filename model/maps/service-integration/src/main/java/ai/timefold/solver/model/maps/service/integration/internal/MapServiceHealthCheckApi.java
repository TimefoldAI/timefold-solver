package ai.timefold.solver.model.maps.service.integration.internal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

public interface MapServiceHealthCheckApi {

    @GET
    @Path("/q/health")
    Response health();

}
