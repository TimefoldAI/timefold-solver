package ai.timefold.solver.service.maps.service.rest.impl;

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.error.ErrorInfo;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.validation.Issue;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.impl.solver.SolverWorkerFacade;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.maps.api.model.Waypoints;
import ai.timefold.solver.service.maps.service.integration.impl.WaypointsService;
import ai.timefold.solver.service.rest.impl.AbstractModelAPIResource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AbstractMapsModelAPIResource<ModelInput_ extends ModelInput, ModelOutput_ extends ModelOutput, ModelConfigurationOverrides_ extends ModelConfigOverrides, Score_ extends Score<?>, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, Justification_ extends ModelConstraintJustification, ValidationIssue_ extends Issue>
        extends
        AbstractModelAPIResource<ModelInput_, ModelOutput_, ModelConfigurationOverrides_, Score_, InputMetrics_, OutputMetrics_, Justification_, ValidationIssue_> {

    private WaypointsService waypointsService;

    public AbstractMapsModelAPIResource() {
        super();
    }

    public AbstractMapsModelAPIResource(ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator,
            SolverWorkerFacade solverWorkerFacade,
            ValidationIssueTypeCatalog validationIssueTypeCatalog,
            WaypointsService waypointsService) {
        super(modelValidator, solverWorkerFacade, validationIssueTypeCatalog);
        this.waypointsService = waypointsService;
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Waypoints successfully calculated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Waypoints.class))) })
    @Operation(operationId = "getWaypoints",
            summary = "Calculates waypoints for problem with supplied identifier")
    @GET
    @Path("/{id}/waypoints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWaypoints(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id,
            @Parameter(description = "Unique identifier of the object waypoints should be collected for",
                    required = false) @QueryParam("objectId") Set<String> objectIds) {

        return Response.ok(waypointsService.getWaypoints(id, objectIds)).build();

    }
}
