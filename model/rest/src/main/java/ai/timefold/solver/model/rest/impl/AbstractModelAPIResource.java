package ai.timefold.solver.model.rest.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelConstraintJustification;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.SolvingStatus;
import ai.timefold.solver.model.definition.api.domain.Configuration;
import ai.timefold.solver.model.definition.api.domain.Metadata;
import ai.timefold.solver.model.definition.api.domain.ModelInputPatchRequest;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;
import ai.timefold.solver.model.definition.api.domain.ModelResponse;
import ai.timefold.solver.model.definition.api.domain.RunConfiguration;
import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.api.error.ValidationErrorInfo;
import ai.timefold.solver.model.definition.api.log.LogInfo;
import ai.timefold.solver.model.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.model.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.model.definition.api.rest.DatasetSelector;
import ai.timefold.solver.model.definition.api.rest.OperationId;
import ai.timefold.solver.model.definition.api.rest.OperationOnPost;
import ai.timefold.solver.model.definition.api.validation.Issue;
import ai.timefold.solver.model.definition.api.validation.IssueCode;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;
import ai.timefold.solver.model.definition.api.validation.Validated;
import ai.timefold.solver.model.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.model.definition.api.validation.dto.ValidationIssueTypes;
import ai.timefold.solver.model.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.model.definition.impl.log.LoggingConstants;
import ai.timefold.solver.model.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.model.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.model.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.model.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.model.definition.internal.events.SolverChannels;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.model.json.internal.patch.JsonPatch;

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
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MutinyEmitter;

@RegisterForReflection
@SuppressWarnings("unchecked")
// the Score_ type parameter cannot be bound as `Score_ extends Score<Score_>` due to stack overflow in RESTEasy/Jandex
// cannot use even `Score_ extends Score<?>` as it results in serializing the `Score` class as an object
public abstract class AbstractModelAPIResource<ModelInput_ extends ModelInput, ModelOutput_ extends ModelOutput, ModelConfigurationOverrides_ extends ModelConfigOverrides, Score_, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, Justification_ extends ModelConstraintJustification, ValidationIssue_ extends Issue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractModelAPIResource.class);

    protected final ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator;

    protected AbstractStorageService<ModelInput_, ModelConfigurationOverrides_, InputMetrics_, OutputMetrics_, ModelOutput_, Score_, Justification_> storageService;

    private Emitter<DatasetCreatedEvent> datasetCreatedEventEmitter;

    private Emitter<DatasetValidateComputeCommand> datasetValidateComputeCommandEmitter;

    private Emitter<SolveStartCommand> solveStartCommandEmitter;

    private MutinyEmitter<SolveTerminateCommand> solveTerminateCommandEmitter;

    private ObjectMapper mapper;

    private ValidationIssueTypeCatalog validationIssueTypeCatalog;

    @Inject
    @Channel(SolverChannels.DATASET_EVENTS)
    Multi<Metadata<Score_>> datasetLifeCycleEvents;

    // required for generated implementations
    public AbstractModelAPIResource() {
        modelValidator = null;
    }

    public AbstractModelAPIResource(
            ModelValidator<ModelInput_, ModelConfigurationOverrides_> modelValidator,
            AbstractStorageService storageService,
            Emitter<DatasetCreatedEvent> datasetCreatedEventEmitter,
            Emitter<DatasetValidateComputeCommand> datasetValidateComputeCommandEmitter,
            Emitter<SolveStartCommand> solveStartCommandEmitter,
            MutinyEmitter<SolveTerminateCommand> solveTerminateCommandEmitter,
            ObjectMapper mapper,
            ValidationIssueTypeCatalog validationIssueTypeCatalog) {
        this.modelValidator = modelValidator;
        this.storageService = storageService;
        this.datasetCreatedEventEmitter = datasetCreatedEventEmitter;
        this.datasetValidateComputeCommandEmitter = datasetValidateComputeCommandEmitter;
        this.solveStartCommandEmitter = solveStartCommandEmitter;
        this.solveTerminateCommandEmitter = solveTerminateCommandEmitter;
        this.mapper = mapper;
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
        Metadata<Score_> metadata = new Metadata<>(runName);
        metadata.setTags(tags);
        metadata.datasetCreated();
        storageService.storeProblem(metadata.getId(), request.modelInput(), metadata, request.configuration(),
                request.configuration());
        datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
        datasetValidateComputeCommandEmitter
                .send(new DatasetValidateComputeCommand(metadata.getId(), operation == OperationOnPost.SOLVE));

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
        Metadata<Score_> metadata = storageService().getMetadata(id); // ensure the dataset exists
        if (metadata == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Dataset with id " + id + " was not found");
        }

        solveStartCommandEmitter.send(new SolveStartCommand(id));

        return metadata;
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
        Configuration<ModelConfigurationOverrides_> initialConfiguration = null;

        if (configuration != null && configuration.run() != null) {
            RunConfiguration runConfiguration = configuration.run();
            runName = runConfiguration.name();
            tags = runConfiguration.tags();
            initialConfiguration = configuration;
        } else if (configuration == null) {
            configuration = storageService.getConfiguration(id);
            initialConfiguration = storageService.getUnprocessedConfiguration(id);
        }

        ModelInput_ input =
                select == DatasetSelector.UNSOLVED ? storageService.getModelInput(id) : storageService.getSolvedModelInput(id);
        if (input == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }
        ModelRequest<ModelInput_, ModelConfigurationOverrides_> request =
                new ModelRequest<>(configuration, input);
        Metadata<Score_> metadata = new Metadata<>(name == null ? runName : name);
        metadata.setTags(tags);
        // set parent id to the id this data set is created from

        metadata.setParentId(id);
        // set origin id based on the origin id of the parent data set
        Metadata<Score_> parentRun = storageService.getMetadata(id);
        metadata.setOriginId(parentRun.getOriginId());
        metadata.datasetCreated();
        storageService.storeProblem(metadata.getId(), request.modelInput(), metadata, initialConfiguration,
                request.configuration());
        datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
        datasetValidateComputeCommandEmitter
                .send(new DatasetValidateComputeCommand(metadata.getId(), operation == OperationOnPost.SOLVE));
        return metadata;
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

        ModelInput_ input =
                select == DatasetSelector.UNSOLVED ? storageService.getModelInput(id) : storageService.getSolvedModelInput(id);
        if (input == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }
        try {
            Configuration<ModelConfigurationOverrides_> configuration =
                    patchRequest.config() != null ? patchRequest.config() : storageService.getConfiguration(id);
            Configuration<ModelConfigurationOverrides_> initialConfiguration = storageService.getUnprocessedConfiguration(id);
            if (configuration == null) {
                configuration = Configuration.empty();
                initialConfiguration = Configuration.empty();
            }
            JsonNode inputTree = mapper.valueToTree(input);
            ArrayNode patchTree = mapper.valueToTree(patchRequest.patch());

            JsonNode patchedInput = JsonPatch.apply(patchTree, inputTree);

            ModelInput_ patchedModelInput = (ModelInput_) mapper.treeToValue(patchedInput, input.getClass());

            ModelRequest<ModelInput_, ModelConfigurationOverrides_> request =
                    new ModelRequest<>(configuration, patchedModelInput);
            Metadata<Score_> metadata = new Metadata<>(name);
            // set parent id to the id this data set is created from
            metadata.setParentId(id);
            // set origin id based on the origin id of the parent data set
            Metadata<Score_> parentRun = storageService.getMetadata(id);
            metadata.setOriginId(parentRun.getOriginId());
            metadata.datasetCreated();
            storageService.storeProblem(metadata.getId(), request.modelInput(), metadata, initialConfiguration,
                    request.configuration());
            datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
            datasetValidateComputeCommandEmitter
                    .send(new DatasetValidateComputeCommand(metadata.getId(), operation == OperationOnPost.SOLVE));
            return metadata;

        } catch (IllegalStateException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationErrorInfo(List.of(e.getMessage()))).build());
        } catch (Exception e) {
            throw new TimefoldRuntimeException(ErrorCodes.UNKNOWN, UUID.randomUUID().toString(), e);
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
        // TODO: temporarily use a meta data in the modelOutput
        return storageService.listRuns(pageNumber, pageSize);
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
        return storageService.getModelResponse(id);
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
        return storageService.getModelRequest(id);
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
        return storageService.getModelInput(id);
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
        return storageService.getMetadata(id);
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
        return storageService.getConfiguration(id);
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
        solveTerminateCommandEmitter.sendAndAwait(new SolveTerminateCommand(id));
        return storageService.getModelResponse(id);
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

        LogInfo podLogInfo = null;
        LogInfo logs = storageService.getLogs(id);
        java.nio.file.Path solverLogPath = Paths.get(LoggingConstants.SOLVER_LOG_PATH);
        if (Files.exists(solverLogPath)) {
            try {
                podLogInfo = new LogInfo(
                        Files.readAllLines(solverLogPath).stream().collect(Collectors.joining(System.lineSeparator())));
            } catch (IOException e) {
                LOGGER.warn("Unable to read solver log file at {}: {}", solverLogPath, e.getMessage());
            }
        }

        if (podLogInfo != null && logs != null) {
            podLogInfo.appendPreviousLog(logs.getDetails());
            return podLogInfo;
        } else if (podLogInfo != null) {
            return podLogInfo;
        } else {
            return logs;
        }
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

        Metadata<Score_> metadata = storageService.getMetadata(id);
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
        ValidationBuilder validationBuilder = new ValidationBuilder();
        ModelRequest<ModelInput_, ModelConfigurationOverrides_> modelRequest = storageService.getModelRequest(id);
        if (modelRequest.modelInput() == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }
        modelValidator.validate(validationBuilder, modelRequest.modelInput(), modelRequest.getModelConfig());
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

    public AbstractStorageService storageService() {
        return storageService;
    }
}
