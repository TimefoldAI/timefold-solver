package ai.timefold.solver.service.rest.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolvingStatus;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.definition.api.domain.ModelInputPatchRequest;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.domain.ModelResponse;
import ai.timefold.solver.service.definition.api.domain.RunConfiguration;
import ai.timefold.solver.service.definition.api.error.ErrorInfo;
import ai.timefold.solver.service.definition.api.error.ValidationErrorInfo;
import ai.timefold.solver.service.definition.api.log.LogInfo;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.rest.DatasetSelector;
import ai.timefold.solver.service.definition.api.rest.OperationId;
import ai.timefold.solver.service.definition.api.rest.OperationOnPost;
import ai.timefold.solver.service.definition.api.validation.Issue;
import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.api.validation.Validated;
import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationIssueTypes;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.impl.solver.SolverWorkerFacade;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;

@RegisterForReflection
@SuppressWarnings("unchecked")
// the Score_ type parameter cannot be bound as `Score_ extends Score<Score_>` due to stack overflow in RESTEasy/Jandex
// cannot use even `Score_ extends Score<?>` as it results in serializing the `Score` class as an object
public abstract class AbstractModelAPIResource<ModelInput_ extends ModelInput, ModelOutput_ extends ModelOutput, ModelConfigurationOverrides_ extends ModelConfigOverrides, Score_, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, Justification_ extends ModelConstraintJustification, ValidationIssue_ extends Issue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractModelAPIResource.class);

    protected final ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator;

    protected SolverWorkerFacade solverWorkerFacade;

    private ValidationIssueTypeCatalog validationIssueTypeCatalog;

    @Inject
    @Channel(SolverChannels.DATASET_EVENTS)
    Multi<Metadata<Score_>> datasetLifeCycleEvents;

    // required for generated implementations
    public AbstractModelAPIResource() {
        modelValidator = null;
    }

    // required for CDI injection
    public AbstractModelAPIResource(
            ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator,
            SolverWorkerFacade solverWorkerFacade,
            ValidationIssueTypeCatalog validationIssueTypeCatalog) {
        this.modelValidator = modelValidator;
        this.solverWorkerFacade = solverWorkerFacade;
        this.validationIssueTypeCatalog = validationIssueTypeCatalog;
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "400", description = "In case request given does not meet expectations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = { ErrorInfo.class, ValidationErrorInfo.class }))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "202", description = "Successfully accepted request to post a dataset",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class, properties = {
                                    @SchemaProperty(name = "score", implementation = String.class) }))) })
    @Operation(operationId = OperationId.SCHEDULE,
            summary = "Posts a dataset and optionally requests its solving. Unique identifier is returned that can be used for further operations on the dataset.")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @RequestBody
    public Response post(
            @NotNull @Validated(
                    operationId = OperationId.SCHEDULE) ModelRequest<ModelInput_, ModelConfigurationOverrides_> request,
            @Parameter(
                    description = "Optional name to be given to the dataset, if not provided a name will be generated.") @QueryParam("name") String name,
            @Parameter(
                    description = "Operation to execute on the POST request.") @DefaultValue(OperationOnPost.DEFAULT_OPERATION) @QueryParam("operation") OperationOnPost operation) {

        Set<String> tags = null;
        String runName = null;
        if (request.configuration() != null && request.configuration().run() != null) {
            RunConfiguration runConfiguration = request.configuration().run();
            runName = runConfiguration.name();
            tags = runConfiguration.tags();
        }

        // if not provided in the request payload, use the query parameter
        runName = runName != null ? runName : name;
        var metadata = switch (operation) {
            case OperationOnPost.NONE ->
                solverWorkerFacade.createDataset(runName, tags, request.modelInput(), request.configuration());
            case OperationOnPost.SOLVE ->
                solverWorkerFacade.createAndSolveDataset(runName, tags, request.modelInput(), request.configuration());
        };

        // The dataset is accepted, even if the validation fails, as on the platform, the validation is an asynchronous process.
        return Response.accepted(metadata).build();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "202", description = "Successfully accepted request to solve scheduling problem",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class, properties = {
                                    @SchemaProperty(name = "score", implementation = String.class) }))) })
    @Operation(operationId = OperationId.SOLVE_DATASET,
            summary = "Request solving a dataset under the given unique identifier.")
    @Path("/{id}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    public Metadata<Score_> solve(
            @Parameter(description = "Unique identifier of the dataset", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.solveDataset(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "400", description = "In case request given does not meet expectations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = { ErrorInfo.class, ValidationErrorInfo.class }))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "202", description = "Successfully accepted request to solve scheduling problem",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class, properties = {
                                    @SchemaProperty(name = "score", implementation = String.class) }))) })
    @Operation(operationId = OperationId.RE_SCHEDULE, deprecated = true,
            summary = "(Deprecated endpoint, please use /{id}/from-input instead) Request a problem to be solved, based on a previously solved dataset. A unique identifier is returned that can be used to get the schedule once solved")
    @POST
    @Path("/{id}/new-run")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @RequestBody(required = false)
    @Deprecated
    public Metadata<Score_> reschedule(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id,
            @Validated(nullable = true,
                    operationId = OperationId.RE_SCHEDULE) Configuration<ModelConfigurationOverrides_> configuration) {
        LOGGER.warn("Request received in /{id}/new-run deprecated endpoint, please use /{id}/from-input instead");
        return fromInput(id, null, DatasetSelector.valueOf(DatasetSelector.DEFAULT_SELECTOR),
                OperationOnPost.valueOf(OperationOnPost.DEFAULT_OPERATION),
                configuration);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "400", description = "In case request given does not meet expectations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = { ErrorInfo.class, ValidationErrorInfo.class }))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "202", description = "Successfully accepted request to solve scheduling problem",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class, properties = {
                                    @SchemaProperty(name = "score", implementation = String.class) }))) })
    @Operation(operationId = OperationId.FROM_INPUT,
            summary = "Request a problem to be solved, based on a previously solved dataset. A unique identifier is returned that can be used to get the schedule once solved")
    @POST
    @Path("/{id}/from-input")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @RequestBody(required = false)
    public Metadata<Score_> fromInput(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id,
            @Parameter(
                    description = "Optional name to be given to the dataset, if not provided a name will be generated.") @QueryParam("name") String name,
            @Parameter(
                    description = "Data to use as a source for the operation.") @DefaultValue("UNSOLVED") @QueryParam("select") DatasetSelector select,
            @Parameter(
                    description = "Operation to execute on the POST request.") @DefaultValue(OperationOnPost.DEFAULT_OPERATION) @QueryParam("operation") OperationOnPost operation,
            @Validated(nullable = true,
                    operationId = OperationId.FROM_INPUT) Configuration<ModelConfigurationOverrides_> configuration) {

        Set<String> tags = null;
        String runName = null;

        if (configuration != null && configuration.run() != null) {
            RunConfiguration runConfiguration = configuration.run();
            runName = runConfiguration.name();
            tags = runConfiguration.tags();
        }
        runName = name == null ? runName : name;

        return switch (operation) {
            case OperationOnPost.NONE -> solverWorkerFacade.createDataset(id, select, runName, tags, configuration);
            case OperationOnPost.SOLVE -> solverWorkerFacade.createAndSolveDataset(id, select, runName, tags, configuration);
        };
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "400", description = "In case request given does not meet expectations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(oneOf = { ErrorInfo.class, ValidationErrorInfo.class }))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "202",
                    description = "Successfully patched and accepted request to solve scheduling problem",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class, properties = {
                                    @SchemaProperty(name = "score", implementation = String.class) }))) })
    @Operation(operationId = OperationId.FROM_PATCH,
            description = "Request a problem to be solved, based on a previous dataset that is patched with given operations. This operation is in preview and might be a subject to change. This operation is not be available for everyone (feature-flagged).",
            summary = "Preview: Request a problem to be solved, based on a previous dataset that is patched with given operations. A unique identifier is returned that can be used to get the dataset")
    @POST
    @Path("/{id}/from-patch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @RequestBody
    public Metadata<Score_> fromPatch(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id,
            @Parameter(
                    description = "Optional name to be given to the dataset, if not provided a name will be generated.") @QueryParam("name") String name,
            @Parameter(
                    description = "Data to use as a source for the operation.") @DefaultValue(DatasetSelector.DEFAULT_SELECTOR) @QueryParam("select") DatasetSelector select,
            @Parameter(
                    description = "Operation to execute on the POST request.") @DefaultValue(OperationOnPost.DEFAULT_OPERATION) @QueryParam("operation") OperationOnPost operation,
            @Validated(nullable = true,
                    operationId = OperationId.FROM_PATCH) ModelInputPatchRequest<ModelConfigurationOverrides_> patchRequest) {
        try {
            return switch (operation) {
                case OperationOnPost.NONE -> solverWorkerFacade.patchDataset(id, select, name, patchRequest);
                case OperationOnPost.SOLVE -> solverWorkerFacade.patchAndSolveDataset(id, select, name, patchRequest);
            };
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationErrorInfo(List.of(e.getMessage()))).build());
        }
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling with given identifier",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = SchemaType.ARRAY, implementation = Metadata.class)))
    })
    @Operation(operationId = OperationId.GET_SCHEDULES, deprecated = true,
            summary = "List all schedules that are registered in the service (that are to be solved, in progress or solved), only returning it's status, score and id")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public List<Metadata<Score_>> getSchedules(@QueryParam("page") @DefaultValue("0") @PositiveOrZero int pageNumber,
            @QueryParam("size") @DefaultValue("100") @PositiveOrZero @Max(1000) int pageSize) {
        return solverWorkerFacade.listRuns(pageNumber, pageSize);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling with given identifier") })
    @Operation(operationId = OperationId.GET_SCHEDULE,
            summary = "Get schedule (might be incomplete as long as the solver is still running) with given identifier")
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> getSchedule(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.getModelResponse(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling input with given identifier") })
    @Operation(operationId = OperationId.GET_MODEL_REQUEST,
            summary = "Get complete input request for given schedule with given identifier")
    @GET
    @Path("/{id}/model-request")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelRequest<ModelInput_, ModelConfigurationOverrides_> getModelRequest(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.getModelRequest(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns input dataset for given identifier") })
    @Operation(operationId = OperationId.GET_SCHEDULE_INPUT,
            summary = "Get input dataset")
    @GET
    @Path("/{id}/input")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelInput_ getScheduleInput(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return (ModelInput_) solverWorkerFacade.getModelInput(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling with given identifier",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class))) })
    @Operation(operationId = OperationId.GET_SCHEDULE_STATUS, deprecated = true,
            summary = "(Deprecated endpoint, please use /{id}/metadata instead) Get schedule status with given identifier")
    @GET
    @Path("/{id}/run")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Metadata<Score_> getScheduleStatus(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        LOGGER.warn("Request received in /{id}/run deprecated endpoint, please use /{id}/metadata instead");
        return getMetadata(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling with given identifier",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = Metadata.class))) })
    @Operation(operationId = OperationId.GET_METADATA, summary = "Get schedule status with given identifier")
    @GET
    @Path("/{id}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Metadata<Score_> getMetadata(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.getMetadata(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200",
                    description = "Returns the configuration used for a schedule with the given identifier") })
    @Operation(operationId = OperationId.GET_SCHEDULE_CONFIG,
            summary = "Get the configuration used for the schedule")
    @GET
    @Path("/{id}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Configuration<ModelConfigurationOverrides_> getScheduleConfiguration(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.getConfiguration(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns scheduling with given identifier") })
    @Operation(operationId = OperationId.TERMINATE_SCHEDULE, summary = "Terminate and return schedule with given identifier")
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> terminateSchedule(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.terminate(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given schedule does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Returns logs with given identifier",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = LogInfo.class))) })
    @Operation(operationId = OperationId.GET_SCHEDULE_LOGS, summary = "Get logs with given identifier")
    @GET
    @Path("/{id}/logs")
    @Produces(MediaType.APPLICATION_JSON)
    public LogInfo getScheduleLogs(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        return solverWorkerFacade.getLogs(id);
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case request given dataset does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "410", description = "In case given dataset is already in final state",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Event stream of dataset changes",
                    content = @Content(mediaType = MediaType.SERVER_SENT_EVENTS,
                            schema = @Schema(implementation = Metadata.class))) })
    @Operation(operationId = OperationId.GET_METADATA_EVENTS,
            summary = "Subscribes to events of given dataset (of type metadata) as long as it has not yet reached final state (completed, failed, incomplete or invalid)")
    @GET
    @Path("/{id}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Metadata<Score_>> streamMetadata(
            @PathParam("id") String id,
            @QueryParam("status") List<SolvingStatus> statusFilter) {

        Metadata<Score_> metadata = solverWorkerFacade.getMetadata(id);
        if (metadata == null) {
            throw new ItemNotFoundException(ErrorCodes.NOT_FOUND, "Dataset " + id + " was not found");
        }

        if (metadata.getSolverStatus() == SolvingStatus.DATASET_INVALID
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_COMPLETED
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_INCOMPLETE
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_FAILED) {
            throw new WebApplicationException(
                    "Dataset " + id + " is already in final state " + metadata.getSolverStatus(),
                    Response.Status.GONE);
        }
        // keep track of final states to avoid sending duplicated events
        AtomicBoolean finalEventProcessed = new AtomicBoolean(false);

        return datasetLifeCycleEvents.select().where(item -> item != null && !finalEventProcessed.get())
                .onFailure().recoverWithCompletion()
                .map(event -> {

                    SolvingStatus status = event.getSolverStatus();
                    if (status == SolvingStatus.SOLVING_COMPLETED
                            || status == SolvingStatus.SOLVING_INCOMPLETE
                            || status == SolvingStatus.SOLVING_FAILED) {
                        finalEventProcessed.set(true);
                    }
                    return event;
                })
                .filter(event -> event.getId().equals(id))
                .filter(event -> statusFilter == null || statusFilter.isEmpty()
                        || statusFilter.contains(event.getSolverStatus()));
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case the given dataset does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Validation results of a dataset with the given identifier") })
    @Operation(operationId = OperationId.GET_VALIDATION_RESULT,
            summary = "Get validation result of a dataset with the given identifier")
    @GET
    @Path("/{id}/validation-result")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResult<ValidationIssue_> getValidationResult(
            @Parameter(description = "Unique identifier of the schedule", required = true) @PathParam("id") String id) {
        ModelInput_ modelInput = (ModelInput_) solverWorkerFacade.getModelInput(id);
        if (modelInput == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }
        var modelConfig = Configuration.getSafeModelConfig(solverWorkerFacade.getConfiguration(id));
        ValidationBuilder validationBuilder = new ValidationBuilder();
        modelValidator.validate(validationBuilder, modelInput, (ModelConfig<ModelConfigurationOverrides_>) modelConfig);
        return validationBuilder.build();
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Validation issue types supported by the model") })
    @Operation(operationId = OperationId.GET_VALIDATION_ISSUE_TYPES,
            summary = "Get validation issue types supported by the model")
    @GET
    @Path("/validation-issue-types")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationIssueTypes getValidationIssueTypes() {
        return new ValidationIssueTypes(validationIssueTypeCatalog.getIssueTypes());
    }

    @APIResponses(value = {
            @APIResponse(responseCode = "404", description = "In case the issue type of the given code does not exist",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "500", description = "In case of processing errors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ErrorInfo.class))),
            @APIResponse(responseCode = "200", description = "Validation issue type of the given code",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ValidationIssueTypes.class))) })
    @Operation(operationId = OperationId.GET_VALIDATION_ISSUE_TYPE_BY_CODE,
            summary = "Get validation issue type by the code")
    @GET
    @Path("/validation-issue-types/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getValidationIssueTypeByCode(
            @Parameter(description = "Unique identifier of the validation issue type",
                    required = true) @PathParam("code") String code) {
        IssueCode issueCode = IssueCode.of(code);
        for (var issueType : validationIssueTypeCatalog.getIssueTypes()) {
            if (issueType.code().equals(issueCode)) {
                return Response.ok().entity(issueType).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
