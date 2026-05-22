package ai.timefold.solver.model.maps.service.rest.impl;

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelConstraintJustification;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.model.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.model.definition.api.validation.Issue;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;
import ai.timefold.solver.model.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.model.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.model.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.model.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.model.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.maps.api.model.Waypoints;
import ai.timefold.solver.model.maps.service.integration.impl.WaypointsService;
import ai.timefold.solver.model.rest.impl.AbstractModelAPIResource;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.reactive.messaging.MutinyEmitter;

@RegisterForReflection
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AbstractMapsModelAPIResource<ModelInput_ extends ModelInput, ModelOutput_ extends ModelOutput, ModelConfigurationOverrides_ extends ModelConfigOverrides, Score_ extends Score<?>, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, Justification_ extends ModelConstraintJustification, ValidationIssue_ extends Issue>
        extends
        AbstractModelAPIResource<ModelInput_, ModelOutput_, ModelConfigurationOverrides_, Score_, InputMetrics_, OutputMetrics_, Justification_, ValidationIssue_> {

    private WaypointsService waypointsService;

    public AbstractMapsModelAPIResource() {
        super();
    }

    public AbstractMapsModelAPIResource(ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator,
            AbstractStorageService storageService,
            Emitter<DatasetCreatedEvent> datasetPostedEventEmitter,
            Emitter<DatasetValidateComputeCommand> datasetValidateComputeCommandEmitter,
            Emitter<SolveStartCommand> scheduleStartEmitter,
            MutinyEmitter<SolveTerminateCommand> scheduleTerminateEmitter,
            ObjectMapper mapper,
            ValidationIssueTypeCatalog validationIssueTypeCatalog,
            WaypointsService waypointsService) {
        super(modelValidator, storageService, datasetPostedEventEmitter,
                datasetValidateComputeCommandEmitter, scheduleStartEmitter, scheduleTerminateEmitter, mapper,
                validationIssueTypeCatalog);
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
