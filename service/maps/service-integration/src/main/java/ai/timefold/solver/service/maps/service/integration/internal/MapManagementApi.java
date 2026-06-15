package ai.timefold.solver.service.maps.service.integration.internal;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.timefold.solver.service.maps.service.integration.internal.model.LocationSet;
import ai.timefold.solver.service.maps.service.integration.internal.model.LocationSetStatus;
import ai.timefold.solver.service.maps.service.integration.internal.model.PatchLocationSet;

public interface MapManagementApi {

    @Path("/v1/management/location-sets")
    @POST
    @Consumes(APPLICATION_JSON)
    void saveLocationSet(@Valid LocationSet locationSet);

    @Path("/v1/management/location-sets")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    void patchLocationSet(@Valid PatchLocationSet updateLocations);

    @Produces(MediaType.APPLICATION_JSON)
    @Path("/v1/management/location-sets/{tenant}/{provider}/{locationSetName}/status")
    @GET
    LocationSetStatus getLocationSetStatus(@PathParam("tenant") UUID tenant, @PathParam("provider") String provider,
            @PathParam("locationSetName") String locationSetName);

    @Path("/v1/management/location-sets")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void clearLocationSets();

    @DELETE
    @Path("/v1/management/location-sets/{tenant}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void clearLocationSetsByTenant(@PathParam("tenant") UUID tenant);

    @DELETE
    @Path("/v1/management/location-sets/{tenant}/{provider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void cleanLocationSetsByProvider(@PathParam("tenant") UUID tenant, @PathParam("provider") String provider);

    @DELETE
    @Path("/v1/management/location-sets/{tenant}/{provider}/{locationSetName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void cleanLocationSetsById(@PathParam("tenant") UUID tenant, @PathParam("provider") String provider,
            @PathParam("locationSetName") String locationSetName);

}
