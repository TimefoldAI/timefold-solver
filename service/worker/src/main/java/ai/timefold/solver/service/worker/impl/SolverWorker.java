package ai.timefold.solver.service.worker.impl;

import static ai.timefold.solver.service.definition.internal.error.ErrorCodes.SOLVER_UNKNOWN;
import static ai.timefold.solver.service.definition.internal.platform.EnvironmentVars.ENV_TIMEFOLD_PLAN_NAME;
import static ai.timefold.solver.service.definition.internal.platform.EnvironmentVars.ENV_TIMEFOLD_TENANT_NAME;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverConfigOverride;
import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;
import ai.timefold.solver.service.definition.api.ModelConvertor;
import ai.timefold.solver.service.definition.api.ModelConvertorBase;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.SolvingStatus;
import ai.timefold.solver.service.definition.api.Status;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnricherService;
import ai.timefold.solver.service.definition.api.enrichment.SolverModelEnrichmentDirectorService;
import ai.timefold.solver.service.definition.api.metrics.InputMetricsAware;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.metrics.OutputMetricsAware;
import ai.timefold.solver.service.definition.api.validation.LegacyValidationResult;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;
import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.internal.MapEnrichmentContext;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.definition.internal.events.AbstractEvent;
import ai.timefold.solver.service.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetComputedEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.service.definition.internal.events.DatasetValidatedEvent;
import ai.timefold.solver.service.definition.internal.events.FailedSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.ItemCompleted;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.definition.internal.events.ItemStarted;
import ai.timefold.solver.service.definition.internal.events.ItemTerminated;
import ai.timefold.solver.service.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.service.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.definition.internal.events.ValidationSummary;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.service.definition.internal.platform.OnStartCommand;
import ai.timefold.solver.service.definition.internal.solver.BestSolutionConsumerDecorator;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.worker.impl.termination.TerminationService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.All;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Broadcast;

@SuppressWarnings({ "unchecked", "rawtypes" })
@ApplicationScoped
public class SolverWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolverWorker.class);
    private static final long COMPLETION_TIMEOUT = 60_000;
    private static final long EMITTER_TIMEOUT = 5;

    private Optional<String> modelName;
    private Optional<String> modelVersion;
    private Optional<String> applicationVersion;

    private final AbstractStorageService storageService;

    private final ModelValidator modelValidator;

    private final SolverManager<SolverModel> solverManager;

    // Intentionally using the SimpleScore just to get the bean injected. The real score type is model-specific.
    private final SolutionManager<SolverModel, SimpleScore> solutionManager;

    private final ModelConvertor modelConvertor;

    private final SolverModelEnricherService enricherService;

    private final SolverModelEnrichmentDirectorService enrichmentDirectorService;

    private final MapEnrichmentContext mapEnrichmentContext;

    private final TerminationService terminationService;

    private final Emitter<DatasetValidatedEvent> datasetValidatedEventEmitter;

    private final Emitter<DatasetComputedEvent> datasetOutputsComputedEmitter;

    private final Emitter<ItemStarted> scheduleStartedEmitter;

    private final Emitter<ItemTerminated> scheduleTerminatedEmitter;

    private final Emitter<ItemCompleted> scheduleCompletedEmitter;

    private final Emitter<ItemFailed> scheduleFailedEmitter;

    private final Emitter<InitSolutionEvent> initSolutionEmitter;

    private final Emitter<BestSolutionEvent> bestSolutionEmitter;

    private final Emitter<FinalBestSolutionEvent> finalSolutionEmitter;

    private final Emitter<FailedSolutionEvent> failedSolutionEmitter;

    private final List<ModelPostProcessor> modelPostProcessors;

    private final ShutdownExecutor shutdownExecutor;

    private final ShutdownOnTerminate shutdownOnTerminate;

    private final CompletionStatus completionStatus;

    private final ConcurrentMap<Object, CompletableFuture<SolverJob<SolverModel>>> solverJobs = new ConcurrentHashMap<>();

    private final String planName;

    private final String tenantName;

    private BestSolutionConsumerDecorator bestSolutionConsumerDecorator;

    private AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private BroadcastProcessor<Metadata<?>> processor = BroadcastProcessor.create();

    @Inject
    public SolverWorker(@ConfigProperty(name = "timefold.application.name") Optional<String> modelName,
            @ConfigProperty(name = "timefold.application.version") Optional<String> modelVersion,
            @ConfigProperty(name = "quarkus.application.version") Optional<String> applicationVersion,
            AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService,
            ModelValidator<?, ?> modelValidator,
            @All List<ModelPostProcessor> modelPostProcessors,
            SolverManager<SolverModel> solverManager,
            SolutionManager<SolverModel, SimpleScore> solutionManager,
            ModelConvertorBase modelConvertor,
            SolverModelEnricherService enricherService,
            SolverModelEnrichmentDirectorService enrichmentDirectorService,
            MapEnrichmentContext mapEnrichmentContext,
            TerminationService terminationService,
            ShutdownExecutor shutdownExecutor,
            ShutdownOnTerminate shutdownOnTerminate,
            Instance<BestSolutionConsumerDecorator> bestSolutionConsumerDecoratorOptional,
            CompletionStatus completionStatus,
            @Broadcast @Channel(SolverChannels.DATASET_COMPUTED) Emitter<DatasetComputedEvent> datasetOutputsComputedEmitter,
            @Broadcast @Channel(SolverChannels.DATASET_VALIDATED) Emitter<DatasetValidatedEvent> datasetValidatedEventEmitter,
            @Broadcast @Channel(SolverChannels.STARTED) Emitter<ItemStarted> scheduleStartedEmitter,
            @Broadcast @Channel(SolverChannels.TERMINATED) Emitter<ItemTerminated> scheduleTerminatedEmitter,
            @Broadcast @Channel(SolverChannels.COMPLETED) Emitter<ItemCompleted> scheduleCompletedEmitter,
            @Broadcast @Channel(SolverChannels.FAILED) Emitter<ItemFailed> scheduleFailedEmitter,
            @Broadcast @Channel(SolverChannels.INIT_SOLUTION) Emitter<InitSolutionEvent> initSolutionEmitter,
            @Broadcast @Channel(SolverChannels.BEST_SOLUTION) Emitter<BestSolutionEvent> bestSolutionEmitter,
            @Broadcast @Channel(SolverChannels.FINAL_BEST_SOLUTION) Emitter<FinalBestSolutionEvent> finalSolutionEmitter,
            @Broadcast @Channel(SolverChannels.FAILED_SOLUTION) Emitter<FailedSolutionEvent> failedSolutionEmitter) {
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.applicationVersion = applicationVersion;
        this.storageService = storageService;
        this.modelValidator = modelValidator;
        this.modelPostProcessors = modelPostProcessors;
        this.solverManager = solverManager;
        this.solutionManager = solutionManager;
        this.modelConvertor = (ModelConvertor) modelConvertor;
        this.enricherService = enricherService;
        this.enrichmentDirectorService = enrichmentDirectorService;
        this.mapEnrichmentContext = mapEnrichmentContext;
        this.terminationService = terminationService;
        this.shutdownExecutor = shutdownExecutor;
        this.shutdownOnTerminate = shutdownOnTerminate;
        this.completionStatus = completionStatus;

        if (bestSolutionConsumerDecoratorOptional.isResolvable()) {
            bestSolutionConsumerDecorator = bestSolutionConsumerDecoratorOptional.get();
        }

        this.datasetOutputsComputedEmitter = datasetOutputsComputedEmitter;
        this.datasetValidatedEventEmitter = datasetValidatedEventEmitter;
        this.scheduleStartedEmitter = scheduleStartedEmitter;
        this.scheduleTerminatedEmitter = scheduleTerminatedEmitter;
        this.scheduleCompletedEmitter = scheduleCompletedEmitter;
        this.scheduleFailedEmitter = scheduleFailedEmitter;
        this.initSolutionEmitter = initSolutionEmitter;
        this.bestSolutionEmitter = bestSolutionEmitter;
        this.finalSolutionEmitter = finalSolutionEmitter;
        this.failedSolutionEmitter = failedSolutionEmitter;
        this.planName = System.getenv(ENV_TIMEFOLD_PLAN_NAME);
        this.tenantName = System.getenv(ENV_TIMEFOLD_TENANT_NAME);
    }

    private void reportExecutionEnvironmentInfo() {

        var nodeName = System.getenv(EnvironmentVars.K8S_INFO_NODE_NAME);
        var memoryLimit = System.getenv(EnvironmentVars.K8S_INFO_MEMORY_LIMIT);
        var totalMemory = String.valueOf(Runtime.getRuntime().maxMemory());

        var java = System.getProperty("java.version");
        var osArch = System.getProperty("os.arch");
        var os = System.getProperty("os.name");
        var cores = String.valueOf(Runtime.getRuntime().availableProcessors());

        if (modelName.isPresent() && modelVersion.isPresent() && applicationVersion.isPresent()) {
            LOGGER.info("Model {} {} ({})", modelName.get(), modelVersion.get(), applicationVersion.get());
        }
        LOGGER.info(
                "Execution environment: java: '{}', arch: '{}', os: '{}', available cpu: '{}', available memory: '{} (limit {})', node name: '{}'",
                java, osArch, os, cores, totalMemory, memoryLimit, nodeName);
    }

    private void sendEvent(Emitter emitter, AbstractEvent event) {
        try {
            emitter.send(event).toCompletableFuture().get(EMITTER_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Error sending event: {}, message: {}", event, e.getMessage(), e);
        }
    }

    /**
     * This method is an entry point for the platform to start the solver worker.
     * <p>
     * It reads the environment variables to determine the job ID and the command to execute.
     *
     * @param event the startup event
     */
    public void onStart(@Observes StartupEvent event) {
        reportExecutionEnvironmentInfo();
        var id = System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID);
        var onStartCommandEnv = System.getenv(EnvironmentVars.ENV_TIMEFOLD_ON_START_COMMAND);
        if (id == null && onStartCommandEnv == null) {
            LOGGER.atDebug().log("No environment variables specified; assuming the solver worked is running locally.");
            return;
        }

        // If at least one of the environment variables is set, we assume the solver worker is running in the platform.

        if (onStartCommandEnv == null) {
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_DATA,
                    "Environment variable %s is not set, unable to proceed."
                            .formatted(EnvironmentVars.ENV_TIMEFOLD_ON_START_COMMAND),
                    true);
        }

        var onStartCommand = OnStartCommand.valueOf(onStartCommandEnv);

        if (onStartCommand == OnStartCommand.IDLE) {
            startIdle();
            return;
        }

        if (id == null) {
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_DATA,
                    "Environment variable %s is not set, unable to proceed.".formatted(EnvironmentVars.ENV_TIMEFOLD_JOB_ID),
                    true);
        }

        var metadata = storageService.getMetadata(id);
        // if the dataset is in any of the final states, return/shutdown immediately
        if (metadata.getSolverStatus() == SolvingStatus.DATASET_INVALID
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_FAILED
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_COMPLETED
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_INCOMPLETE) {
            shutdownExecutor.scheduleShutdown(Duration.ofSeconds(1), 0);
            return;
        }

        if (onStartCommand == OnStartCommand.SOLVE
                // in case the run was interrupted and is resumed
                || (onStartCommand == OnStartCommand.VALIDATE_COMPUTE_SOLVE
                        && metadata.getSolverStatus() == SolvingStatus.SOLVING_ACTIVE)) {
            LOGGER.atDebug()
                    .log("Skipping validation and computation for a dataset ({}) as the command is {}", id,
                            OnStartCommand.SOLVE);
            startSolvingOnApplicationStart(id);
            return;
        }

        onDatasetValidateComputeCommand(
                new DatasetValidateComputeCommand(id, onStartCommand == OnStartCommand.VALIDATE_COMPUTE_SOLVE));

        if (onStartCommand == OnStartCommand.VALIDATE_COMPUTE) {
            // once the outputs are calculated shutdown
            shutdownExecutor.scheduleShutdown(Duration.ofSeconds(2), 0);
        }
    }

    /**
     * Starts the solver worker in an idle state, to wait for follow-up calls.
     * <p>
     * The shutdown delay can be configured via the environment variable
     * {@link EnvironmentVars#ENV_TIMEFOLD_IDLE_RUNTIME_TTL}.
     */
    private void startIdle() {
        var shutdownDelay = Duration.ofMinutes(10);
        var delayDurationEnv = System.getenv(EnvironmentVars.ENV_TIMEFOLD_IDLE_RUNTIME_TTL);
        if (delayDurationEnv != null) {
            shutdownDelay = Duration.parse(delayDurationEnv);
        }
        shutdownExecutor.scheduleShutdown(shutdownDelay, 0);
    }

    private void computeOutputs(String id, boolean solveRequested) {
        LOGGER.info("Requesting solver for id {} to compute outputs...", id);
        try {
            var metadata = storageService.getMetadata(id);
            var modelInput = storageService.getModelInput(id);
            if (modelInput == null) {
                logUnreadableInput(id);
                return;
            }
            var configuration = storageService.getConfiguration(id);

            var modelConfig = Configuration.getSafeModelConfig(configuration);

            var solverModel = createSolverModel(modelInput, modelConfig);

            String configuredLocation = System.getenv(EnvironmentVars.ENV_TIMEFOLD_PLATFORM_MAP_SERVICE_LOCATION);
            if (EnvironmentVars.MAP_SERVICE_LOCATION_AUTO_SELECT.equalsIgnoreCase(configuredLocation)) {
                LOGGER.info("Auto-select map resolved to '{}' for dataset {}.", mapEnrichmentContext.getResolvedMapLocation(),
                        metadata.getId());
            }

            solutionManager.update(solverModel);

            // Store the updated solution
            var modelOutput = convertToModelOutput(id, solverModel);
            metadata.datasetComputed();
            storageService.storeSolution(id, modelOutput, metadata, extractInputMetrics(solverModel),
                    extractOutputMetrics(solverModel));

            postProcessOutput(id, modelOutput, solverModel);

            sendEvent(datasetOutputsComputedEmitter,
                    new DatasetComputedEvent(metadata, solverModel, planName, tenantName, solveRequested,
                            mapEnrichmentContext.getResolvedMapLocation()));
        } catch (Throwable e) {
            notifyOnFailure(id, e);
        }
    }

    private void logUnreadableInput(String id) {
        LOGGER.error("Unable to read a model input for id {}.\n Maybe check the validation result.", id);
    }

    private void startSolvingOnApplicationStart(String id) {
        try {
            Status status = storageService.getMetadata(id);
            LOGGER.info("Checking status for {} in status {}", id,
                    status.getSolverStatus());
            if (status.getSolverStatus() == SolvingStatus.SOLVING_ACTIVE
                    || status.getSolverStatus() == SolvingStatus.SOLVING_STARTED
                    || status.getSolverStatus() == SolvingStatus.SOLVING_SCHEDULED
                    || status.getSolverStatus() == SolvingStatus.DATASET_COMPUTED) {
                LOGGER.info("Automatically starting/resuming solving for {} in status {}", id,
                        status.getSolverStatus());
                onSolveStartCommand(new SolveStartCommand(id));
            } else if (status.getSolverStatus() == SolvingStatus.SOLVING_FAILED) {
                LOGGER.error("Dataset is {} in failed status {}, exiting", id, status.getSolverStatus());
                shutdownExecutor.scheduleShutdown(Duration.ZERO, 0);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to get status of a request to be processed", e);
            notifyOnFailure(id, e);
        }
    }

    /**
     * Solves a valid dataset.
     * <p>
     * Dataset must be validated before calling this method.
     *
     * @param metadata metadata of the dataset to start solving for; never null
     * @param modelInput the model input to solve; never null
     * @param configuration the configuration to use; can be null
     */
    private void solve(Metadata metadata, ModelInput modelInput, Configuration configuration) {
        final var id = metadata.getId();
        LOGGER.info("Requesting solver for id {} to start...", id);
        try {
            var terminationConfig =
                    terminationService.resolveTerminationConfig(
                            (configuration == null || configuration.run() == null) ? null : configuration.run().termination());
            var solverConfigOverride = new SolverConfigOverride()
                    .withTerminationConfig(terminationConfig);

            var modelConfig = Configuration.getSafeModelConfig(configuration);

            var previousModelOutput = loadModelOutput(id);

            var jobFuture = new CompletableFuture<SolverJob<SolverModel>>();
            solverJobs.put(id, jobFuture);
            try {
                var job = solverManager.solveBuilder()
                        .withProblemFinder(id_ -> notifyOnStart((String) id_, modelInput, previousModelOutput, modelConfig))
                        .withConfigOverride(solverConfigOverride)
                        .withProblemId(id)
                        .withBestSolutionEventConsumer(
                                decorateIfPossible(event -> notifyOnSave(id, event.solution(), event.producerId())))
                        .withFinalBestSolutionEventConsumer(event -> notifyOnComplete(id, event.solution()))
                        .withFirstInitializedSolutionEventConsumer(
                                event -> notifyOnInit(id, event.solution(), event.isTerminatedEarly(), event.producerId()))
                        .withExceptionHandler(this::notifyOnFailure)
                        .run();
                jobFuture.complete(job);
            } catch (Throwable e) {
                jobFuture.completeExceptionally(e);
                throw e;
            }
        } catch (Throwable e) {
            notifyOnFailure(id, e);
        }
    }

    private LegacyValidationResult validateAndUpdateRun(String id) {
        var metadata = storageService.getMetadata(id);

        if (metadata.getSolverStatus() == SolvingStatus.DATASET_VALIDATED
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_ACTIVE
                || metadata.getSolverStatus() == SolvingStatus.DATASET_COMPUTED
                || metadata.getSolverStatus() == SolvingStatus.SOLVING_STARTED) {
            return LegacyValidationResult.successful();
        }

        var modelInput = storageService.getModelInput(id);
        if (modelInput == null) {
            logUnreadableInput(id);
            /*
             * The only case of input being null is when it cannot be parsed, in which case the metadata already
             * contains the updated validation result.
             */
            return storageService.getMetadata(id).getValidationResult();
        }

        var modelConfig = Configuration.getSafeModelConfig(storageService.getConfiguration(id));
        var validationBuilder = new ValidationBuilder();
        modelValidator.validate(validationBuilder, modelInput, modelConfig);

        // We store both the new and old validation result format for backward compatibility.
        ValidationResult validationResponse = validationBuilder.build();
        storageService.storeValidationResponse(id, validationResponse);

        var legacyValidationResult = validationBuilder.buildLegacyValidationResult();
        metadata.datasetValidated(legacyValidationResult);
        storageService.updateMetadata(metadata.getId(), metadata);

        sendEvent(datasetValidatedEventEmitter,
                new DatasetValidatedEvent(metadata, ValidationSummary.of(validationResponse)));
        return legacyValidationResult;
    }

    public void onShutdown(@Observes ShutdownEvent event) {
        this.shuttingDown.set(true);
        var id = System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID);
        if (id != null) {

            try {
                onSolveTerminateCommand(new SolveTerminateCommand(id));
            } catch (ItemNotFoundException e) {
                LOGGER.debug("Solver is already terminated for {}", id);
            }
        }
    }

    @Incoming(SolverChannels.DATASET_VALIDATE_COMPUTE)
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    public void onDatasetValidateComputeCommand(DatasetValidateComputeCommand command) {
        final var id = command.getId();
        try {
            if (!validateAndUpdateRun(id).isValid()) {
                LOGGER.error("Dataset (%s) failed validation. Please check the validation results.".formatted(id));
                shutdownOnTerminate.process(id);
                return;
            }

            computeOutputs(id, command.solve());

            if (command.solve()) {
                onSolveStartCommand(new SolveStartCommand(id));
            }
        } catch (Throwable e) {
            notifyOnFailure(id, e);
        }
    }

    @Incoming(SolverChannels.START)
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    public void onSolveStartCommand(SolveStartCommand command) {
        final var id = command.getId();
        var metadata = storageService.getMetadata(id);
        var modelInput = storageService.getModelInput(id);
        if (modelInput == null) {
            logUnreadableInput(id);
            return;
        }
        var configuration = storageService.getConfiguration(id);

        processor.onNext(metadata);

        solve(metadata, modelInput, configuration);
    }

    @Incoming(SolverChannels.TERMINATE)
    @Blocking
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public void onSolveTerminateCommand(SolveTerminateCommand command) {
        if (this.shuttingDown.get()) {
            LOGGER.debug("Request to terminate solver - Is already shutdown...");
            return;
        }

        var id = command.getId();
        LOGGER.info("Request to terminate solver has been received for id {}", id);
        if (!solverManager.getSolverStatus(id).equals(SolverStatus.NOT_SOLVING)) {
            completionStatus.initiateCompletion(id);
            solverManager.terminateEarly(id);
            boolean countReachedZero;
            try {
                countReachedZero = completionStatus.waitForCompletion(id, COMPLETION_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TimefoldRuntimeException(SOLVER_UNKNOWN, "Unknown error while waiting for solver completion",
                        e, true);
            }
            if (!countReachedZero) {
                throw new TimefoldRuntimeException(ErrorCodes.SOLVER_TOO_MUCH_TIME_TERMINATING_ERROR,
                        "Solver took too much time to terminate", true);
            }
            LOGGER.info("Solving has been terminated for id {}", id);
            sendEvent(scheduleTerminatedEmitter, new ItemTerminated(id));
        } else {
            LOGGER.warn("Solver run for id {} was not found or is not solving", id);
        }
    }

    @Outgoing(SolverChannels.DATASET_EVENTS)
    public Multi<Metadata<?>> forwardLifeCycleEvent() {

        return processor;
    }

    protected SolverModel notifyOnStart(String id, ModelInput modelInput, ModelOutput modelOutput, ModelConfig modelConfig) {
        try {
            LOGGER.debug("Notify run start for id {}", id);
            var metadata = storageService.getMetadata(id);

            var solverModel = createSolverModel(modelInput, modelConfig, modelOutput);
            if (metadata.getSolverStatus() == SolvingStatus.DATASET_COMPUTED
                    || metadata.getSolverStatus() == SolvingStatus.SOLVING_SCHEDULED) {
                metadata.solvingStarted();
                storageService.storeMetadata(id, metadata);
                processor.onNext(metadata);
                sendEvent(scheduleStartedEmitter, new ItemStarted(metadata, solverModel, planName, tenantName));
            }
            if (metadata.getSolverStatus() == SolvingStatus.SOLVING_STARTED) {
                metadata.solvingActive();
                storageService.storeMetadata(id, metadata);
                processor.onNext(metadata);
                // Re-sending item started with new ACTIVE status to reflect stored status, planName, tenantName
                sendEvent(scheduleStartedEmitter, new ItemStarted(metadata, solverModel, planName, tenantName));
            }
            solutionManager.update(solverModel);
            return solverModel;
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            notifyOnFailure(id, e);
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_DATA,
                    "Unable to load dataset with id '" + id + "' due to: " + e.getMessage(), e, true);
        }
    }

    private SolverModel createSolverModel(ModelInput modelInput, ModelConfig modelConfig, ModelOutput modelOutput) {
        try {
            var solverModel =
                    modelConvertor.toSolverModel(modelInput, modelConfig, Optional.ofNullable(modelOutput));
            return enrichModel(solverModel);
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_DATA,
                    "Unable to convert and enrich solver model due to: " + e.getMessage(), e, false);
        }
    }

    private SolverModel createSolverModel(ModelInput modelInput, ModelConfig modelConfig) {
        return createSolverModel(modelInput, modelConfig, null);
    }

    private SolverModel enrichModel(SolverModel solverModel) {
        // TODO handle the check within the enrichment director service https://github.com/TimefoldAI/timefold-orbit/issues/654
        // if there is a compatible model-specific enrichment director, use it, otherwise fall back to the generic enricher service
        return enrichmentDirectorService.accepts(solverModel) ? enrichmentDirectorService.enrich(solverModel)
                : enricherService.enrich(solverModel);
    }

    protected void notifyOnInit(String id, SolverModel solverModel, boolean isTerminatedEarly,
            EventProducerId eventProducerId) {
        LOGGER.debug("Notify run init for id {}", id);
        if (isTerminatedEarly) {
            LOGGER.warn("Initial solution for id {} incomplete because terminated early", id);
        }

        var metadata = storageService.getMetadata(id);
        var solverJob = resolveJob(solverJobs.get(id));
        if (metadata != null && SolvingStatus.SOLVING_ACTIVE == metadata.getSolverStatus() && solverJob != null) {
            var modelOutput = convertToModelOutput(id, solverModel);

            metadata.updateStatusOnSave(SolvingStatus.SOLVING_ACTIVE, solverModel.getScore());
            storageService.updateSolution(id, modelOutput, metadata, extractInputMetrics(solverModel),
                    extractOutputMetrics(solverModel));
            sendEvent(initSolutionEmitter, new InitSolutionEvent(metadata, solverModel,
                    new SolverWorkerJobState(SolverStatus.SOLVING_ACTIVE, solverJob), planName, tenantName,
                    eventProducerId.producerId()));
        }
    }

    protected void notifyOnSave(String id, SolverModel solverModel, EventProducerId eventProducerId) {
        LOGGER.debug("Notify run save for id {}", id);
        var metadata = storageService.getMetadata(id);
        var solverJob = resolveJob(solverJobs.get(id));
        if (metadata != null && SolvingStatus.SOLVING_ACTIVE == metadata.getSolverStatus() && solverJob != null) {
            processor.onNext(metadata);
            var modelOutput = convertToModelOutput(id, solverModel);

            metadata.updateStatusOnSave(SolvingStatus.SOLVING_ACTIVE, solverModel.getScore());
            storageService.updateSolution(id, modelOutput, metadata, extractInputMetrics(solverModel),
                    extractOutputMetrics(solverModel));

            sendEvent(bestSolutionEmitter, new BestSolutionEvent(metadata, solverModel,
                    new SolverWorkerJobState(SolverStatus.SOLVING_ACTIVE, solverJob), planName, tenantName,
                    eventProducerId.producerId()));
        }
    }

    protected void notifyOnComplete(String id, SolverModel solverModel) {
        if (this.shuttingDown.get()) {
            // shutting down flag is only set when solver worker is being shutdown - its pod is shutting down
            // but the termination was not issued - either explicitly by terminate early or by solver termination strategy
            // due to that skip sending final events and updating run in the storage - it is not yet completed and should be resumed
            return;
        }
        LOGGER.debug("Notify run complete for id {}", id);
        try {
            // remove it as the first thing so in case any best solution events will arrive while this method is executed they will be discarded
            var solverJob = resolveJob(solverJobs.remove(id));

            if (solverJob == null) {
                return;
            }
            var modelOutput = convertToModelOutput(id, solverModel);
            storeSolvedInput(id, modelOutput);

            var metadata = storageService.getMetadata(id);
            if (metadata.getScore() == null) {
                // If the score is null, a full solution was not found and the CH did not finish
                metadata.updateStatusOnComplete(SolvingStatus.SOLVING_INCOMPLETE, solverModel.getScore());
            } else {
                metadata.updateStatusOnComplete(SolvingStatus.SOLVING_COMPLETED, solverModel.getScore());
            }
            metadata.shutdown();
            storageService.storeSolution(id, modelOutput, metadata, extractInputMetrics(solverModel),
                    extractOutputMetrics(solverModel));

            processor.onNext(metadata);
            sendEvent(finalSolutionEmitter, new FinalBestSolutionEvent(metadata, solverModel,
                    new SolverWorkerJobState(SolverStatus.NOT_SOLVING, solverJob), planName, tenantName));

            postProcessCompleteOutput(id, modelOutput, solverModel);

            sendEvent(scheduleCompletedEmitter, new ItemCompleted(id));

            completionStatus.completed(id);
            // shutdown has to be executed last to ensure everything executed before pod shuts down
            shutdownOnTerminate.process(id);
        } catch (Throwable e) {
            notifyOnFailure(id, e);
        }
    }

    /**
     * Invokes post processors of the solution to compute additional (optional) outputs, like score analysis or waypoints.
     */
    private void postProcessOutput(String id, ModelOutput modelOutput, SolverModel solverModel) {
        for (var processor : modelPostProcessors) {
            try {
                processor.processComputed(modelOutput, solverModel, id);
            } catch (Throwable e) {
                LOGGER.warn("Unexpected error while invoking post processor {} that returned {}",
                        processor.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private void postProcessCompleteOutput(String id, ModelOutput modelOutput, SolverModel solverModel) {
        for (var processor : modelPostProcessors) {
            try {
                processor.process(modelOutput, solverModel, id);
            } catch (Throwable e) {
                LOGGER.warn("Unexpected error while invoking post processor {} that returned {}",
                        processor.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private void storeSolvedInput(String datasetId, ModelOutput modelOutput) {
        var modelInput = storageService.getModelInput(datasetId);
        if (modelInput == null) {
            throw new ItemNotFoundException(datasetId, "Model input not found for id %s".formatted(datasetId));
        }
        var solvedModelInput = modelConvertor.applyOutputToInput(modelInput, modelOutput);
        storageService.storeSolvedModelInput(datasetId, solvedModelInput);
    }

    public void notifyOnFailure(Object id, Throwable throwable) {
        LOGGER.debug("Notify run failure for id {}", id, throwable);
        var problemId = (String) id;
        Metadata metadata = null;
        try {
            // remove it as the first thing so in case any best solution events will arrive while this method is executed they will be discarded
            var solverJob = resolveJob(solverJobs.remove(id));

            // update run status only as failed
            metadata = storageService.getMetadata(problemId);
            metadata.updateStatusOnFailure(throwable.getMessage());
            storageService.storeMetadata(problemId, metadata);

            if (solverJob != null) {
                processor.onNext(metadata);
                sendEvent(failedSolutionEmitter, new FailedSolutionEvent(metadata, solverJob, throwable, planName, tenantName));
            }
        } finally {

            for (var processor : modelPostProcessors) {
                try {
                    processor.processFailed(problemId, throwable);
                } catch (Throwable e) {
                    LOGGER.warn("Unexpected error while invoking post processor {} that returned {}",
                            processor.getClass().getSimpleName(), e.getMessage());
                }
            }

            try {
                if (metadata != null) {
                    metadata.shutdown();
                    storageService.storeMetadata(problemId, metadata);
                }
            } catch (Throwable e) {
                LOGGER.warn("Unexpected error while storing run {} that returned {}", id, e.getMessage());
            }

            try {
                sendEvent(scheduleFailedEmitter, new ItemFailed(metadata, throwable, planName, tenantName));
            } finally {
                completionStatus.completed(problemId);
                // shutdown has to be executed last to ensure everything executed before pod shuts down
                shutdownOnTerminate.processFailed(problemId, throwable);
            }
        }
    }

    private ModelOutput convertToModelOutput(String id, SolverModel solverModel) {
        try {
            return modelConvertor.toModelOutput(solverModel);
        } catch (TimefoldRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new TimefoldRuntimeException(ErrorCodes.INVALID_DATA,
                    "Unable to create model output for dataset with id '" + id + "' due to: " + e.getMessage(), e, false);
        }
    }

    protected ModelOutput loadModelOutput(String id) {
        try {
            return storageService.getModelOutput(id);
        } catch (ItemNotFoundException e) {
            return null;
        } catch (Exception e) {
            LOGGER.error("Unable to get model output for id (%s)".formatted(id), e);
            return null;
        }
    }

    private ModelInputMetrics extractInputMetrics(SolverModel solverModel) {
        if (solverModel instanceof InputMetricsAware<?> inputMetricsAware) {
            return inputMetricsAware.getInputMetrics();
        }
        return null;
    }

    private ModelOutputMetrics extractOutputMetrics(SolverModel solverModel) {
        if (solverModel instanceof OutputMetricsAware<?> outputMetricsAware) {
            return outputMetricsAware.getOutputMetrics();
        }
        return null;
    }

    private <Solution_> Consumer<NewBestSolutionEvent<Solution_>>
            decorateIfPossible(Consumer<NewBestSolutionEvent<Solution_>> consumer) {
        if (bestSolutionConsumerDecorator != null) {
            return bestSolutionConsumerDecorator.decorate(consumer);
        }

        return consumer;
    }

    private SolverJob<SolverModel> resolveJob(CompletableFuture<SolverJob<SolverModel>> jobFuture) {
        if (jobFuture == null) {
            return null;
        }
        try {
            return jobFuture.join();
        } catch (CompletionException e) {
            return null;
        }
    }

}
