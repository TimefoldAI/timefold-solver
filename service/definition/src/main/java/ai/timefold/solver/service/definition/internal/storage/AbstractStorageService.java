package ai.timefold.solver.service.definition.internal.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.SolverInput;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelInputPatchRequest;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.domain.ModelResponse;
import ai.timefold.solver.service.definition.api.log.LogInfo;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.validation.Issue;
import ai.timefold.solver.service.definition.api.validation.LegacyValidationResult;
import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.impl.storage.CompressionUtils;
import ai.timefold.solver.service.definition.impl.storage.StorageObjectMapperWrapper;
import ai.timefold.solver.service.definition.impl.validation.JsonMappingError;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Object oriented facade in front of a {@link Storage}.
 * <p>
 * This service owns everything that requires the knowledge of the persisted types: it serializes objects into content
 * and compresses it before handing it over to the storage, and decompresses and deserializes the content read back
 * from the storage. The storage itself only ever deals with raw streams of bytes.
 *
 * @param <ModelInput_> type representing the data set to be solved
 * @param <ModelConfigOverrides_> type representing model specific configuration overrides
 * @param <InputMetrics_> type representing metrics of the data set to be solved
 * @param <OutputMetrics_> type representing metrics of the solved data set
 * @param <ModelOutput_> type representing a solved data set
 * @param <Score_> type representing the score of a solved data set
 * @param <Justification_> type representing constraint justifications of a solved data set
 */
public abstract non-sealed class AbstractStorageService<ModelInput_ extends ModelInput, ModelConfigOverrides_ extends ModelConfigOverrides, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, ModelOutput_ extends ModelOutput, Score_, Justification_ extends ModelConstraintJustification>
        implements StorageServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageService.class);

    static final long LOCK_TIMEOUT_SECONDS = 60;

    /**
     * Attribute that data stores supporting attributes use to store the status of the data set. Its presence means the
     * complete metadata can be reconstructed from the attributes, without reading the content of the sub model.
     */
    private static final String SOLVER_STATUS_ATTRIBUTE = "solverStatus";

    /**
     * Property that turns the caching of the model output and the metadata on and off.
     */
    public static final String USE_CACHE_PROPERTY = "timefold.storage.use-cache";

    /**
     * Property with the number of attempts a storage operation is given before it is failed, the first one included.
     * One means no retrying at all.
     * <p>
     * The default of five, with the default waits, keeps trying for about seven and a half seconds, which is meant to
     * outlast the handover when the access service in front of the data store is rolled out.
     */
    public static final String RETRY_MAX_ATTEMPTS_PROPERTY = "timefold.storage.retry.max-attempts";

    /**
     * Property with how long to wait before the second attempt; every further wait doubles it up to the maximum.
     */
    public static final String RETRY_INITIAL_DELAY_PROPERTY = "timefold.storage.retry.initial-delay";

    /**
     * Property with the longest a single wait between two attempts may become.
     * <p>
     * Attempts are made while holding the lock of the data set, so the whole budget has to stay well below the
     * {@value #LOCK_TIMEOUT_SECONDS} seconds others are willing to wait for that lock.
     */
    public static final String RETRY_MAX_DELAY_PROPERTY = "timefold.storage.retry.max-delay";

    protected Storage storage;

    private ObjectMapper mapper;
    private boolean useCache = true;
    private final Map<String, ModelOutput_> outputCache = new ConcurrentHashMap<>();
    private final Map<String, Metadata<Score_>> metadataCache = new ConcurrentHashMap<>();

    private int retryMaxAttempts = 5;
    private Duration retryInitialDelay = Duration.ofMillis(500);
    private Duration retryMaxDelay = Duration.ofSeconds(5);

    private final ConcurrentHashMap<String, LockReference> locks = new ConcurrentHashMap<>();
    private final Lock generalLock = new ReentrantLock();

    @SuppressWarnings("unused")
    protected AbstractStorageService() {
        /* Mandatory for recording */
    }

    // For tests
    protected AbstractStorageService(Storage storage, StorageObjectMapperWrapper storageObjectMapperWrapper) {
        this.storage = storage;
        this.mapper = storageObjectMapperWrapper.get();
    }

    @Inject
    protected void setStorage(Instance<Storage> instance) {
        storage = instance.get();
    }

    @Inject
    protected void setStorageObjectMapper(Instance<StorageObjectMapperWrapper> instance) {
        mapper = instance.get().get();
    }

    @Inject
    protected void setCacheEnabled(
            @ConfigProperty(name = USE_CACHE_PROPERTY, defaultValue = "true") boolean useCache) {
        this.useCache = useCache;
    }

    @Inject
    protected void setRetry(
            @ConfigProperty(name = RETRY_MAX_ATTEMPTS_PROPERTY, defaultValue = "5") int maxAttempts,
            @ConfigProperty(name = RETRY_INITIAL_DELAY_PROPERTY, defaultValue = "PT0.5S") String initialDelay,
            @ConfigProperty(name = RETRY_MAX_DELAY_PROPERTY, defaultValue = "PT5S") String maxDelay) {
        this.retryMaxAttempts = Math.max(1, maxAttempts);
        this.retryInitialDelay = Duration.parse(initialDelay);
        this.retryMaxDelay = Duration.parse(maxDelay);
    }

    public void create(String id, StorageConfiguration storageConfiguration) {
        acquireLock(id);
        try {
            runWithRetry(id, "create the storage", () -> this.storage.create(id, storageConfiguration));
        } finally {
            releaseLock(id);
        }
    }

    public void reconfigure(String id, StorageConfiguration storageConfiguration) {
        acquireLock(id);
        try {
            runWithRetry(id, "reconfigure the storage", () -> this.storage.reconfigure(id, storageConfiguration));
        } finally {
            releaseLock(id);
        }
    }

    public void destroy(String id) {
        acquireLock(id);
        try {
            runWithRetry(id, "destroy the storage", () -> this.storage.destroy(id));
            outputCache.clear();
            metadataCache.clear();
        } finally {
            releaseLock(id);
        }
    }

    public ModelInput_ getModelInput(String id) {
        return getModelInput(null, id);
    }

    /**
     * Reads the model input from storage.
     * <p>
     * If there is a JSON mapping exception during deserialization, the metadata is updated with
     * the validation result containing the error and the dataset is marked as invalid.
     *
     * @param storageAddress specifies the storage location
     * @param id dataset ID
     * @return the model input
     */
    public ModelInput_ getModelInput(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return (ModelInput_) readSubModel(storageAddress, id, SubModelKind.MODEL_INPUT, getModelInputClass());
        } catch (TimefoldRuntimeException timefoldRuntimeException) {
            if (timefoldRuntimeException.getCause() instanceof JsonMappingException mappingException) {
                var validationBuilder = new ValidationBuilder().addIssue(new JsonMappingError(mappingException.getMessage()));

                LegacyValidationResult legacyValidationResult = validationBuilder.buildLegacyValidationResult();
                Metadata<Score_> metadata = readMetadata(storageAddress, id);
                if (metadata != null) {
                    metadata.datasetValidated(legacyValidationResult);
                    updateSubModel(storageAddress, id, SubModelKind.METADATA, metadata); // For backward compatibility.
                    storeSubModel(storageAddress, id, SubModelKind.VALIDATION_RESULT, validationBuilder.build());
                    // Avoid re-throwing the exception, since it was already handled.
                    return null;
                } else {
                    LOGGER.warn("Unable to update metadata for dataset ({}) after JSON mapping exception: metadata not found.",
                            id);
                    // Throw the exception as we cannot guarantee updating the metadata.
                    throw timefoldRuntimeException;
                }
            } else {
                // Throw the exception as we cannot handle it.
                throw timefoldRuntimeException;
            }
        } finally {
            releaseLock(id);
        }
    }

    public void storeModelInput(String id, ModelInput_ modelInput) {
        storeModelInput(null, id, modelInput);
    }

    public void storeModelInput(StorageAddress storageAddress, String id, ModelInput_ modelInput) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.MODEL_INPUT, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public ModelInput_ getSolvedModelInput(String id) {
        return getSolvedModelInput(null, id);
    }

    public ModelInput_ getSolvedModelInput(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return (ModelInput_) readSubModel(storageAddress, id, SubModelKind.MODEL_INPUT_SOLVED, getModelInputClass());
        } finally {
            releaseLock(id);
        }
    }

    public void storeSolvedModelInput(String id, ModelInput_ modelInput) {
        storeSolvedModelInput(null, id, modelInput);
    }

    public void storeSolvedModelInput(StorageAddress storageAddress, String id, ModelInput_ modelInput) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.MODEL_INPUT_SOLVED, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public Metadata<Score_> getMetadata(String id) {
        return getMetadata(null, id);
    }

    public Metadata<Score_> getMetadata(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return readMetadata(storageAddress, id);
        } finally {
            releaseLock(id);
        }
    }

    public void storeMetadata(String id, Metadata<Score_> metadata) {
        storeMetadata(null, id, metadata);
    }

    public void storeMetadata(StorageAddress storageAddress, String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public void updateMetadata(String id, Metadata<Score_> metadata) {
        updateMetadata(null, id, metadata);
    }

    public void updateMetadata(StorageAddress storageAddress, String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            updateSubModel(storageAddress, id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public <T extends Issue> void storeValidationResponse(String id, ValidationResult<T> validationResult) {
        storeValidationResponse(null, id, validationResult);
    }

    public <T extends Issue> void storeValidationResponse(StorageAddress storageAddress, String id,
            ValidationResult<T> validationResult) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.VALIDATION_RESULT, validationResult);
        } finally {
            releaseLock(id);
        }
    }

    public ModelOutput_ getModelOutput(String id) {
        return getModelOutput(null, id);
    }

    public ModelOutput_ getModelOutput(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return readModelOutput(storageAddress, id);
        } finally {
            releaseLock(id);
        }
    }

    public void storeModelOutput(String id, ModelOutput_ modelOutput) {
        storeModelOutput(null, id, modelOutput);
    }

    public void storeModelOutput(StorageAddress storageAddress, String id, ModelOutput_ modelOutput) {
        acquireLock(id);
        try {
            StorageContent content = toContent(id, modelOutput);
            writeWithRetry(id, "store the dataset", content, () -> storage.store(storageAddress, id, content));
            cacheModelOutput(id, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    public void updateModelOutput(String id, ModelOutput_ modelOutput) {
        updateModelOutput(null, id, modelOutput);
    }

    public void updateModelOutput(StorageAddress storageAddress, String id, ModelOutput_ modelOutput) {
        acquireLock(id);
        try {
            StorageContent content = toContent(id, modelOutput);
            writeWithRetry(id, "update the dataset", content, () -> storage.update(storageAddress, id, content));
            cacheModelOutput(id, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    public void completeModelOutput(String id, ModelOutput_ modelOutput) {
        completeModelOutput(null, id, modelOutput);
    }

    public void completeModelOutput(StorageAddress storageAddress, String id, ModelOutput_ modelOutput) {
        acquireLock(id);
        try {
            StorageContent content = toContent(id, modelOutput);
            writeWithRetry(id, "complete the dataset", content, () -> storage.complete(storageAddress, id, content));
            cacheModelOutput(id, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    /**
     * Stores everything a data set starts out with as one unit; the lock is held for all of it, so that nobody
     * observes the data set with only some of the parts written. The lock is reentrant, the methods called below take
     * it again.
     */
    public void storeProblem(StorageAddress storageAddress, String id, ModelInput_ modelInput, Metadata<Score_> metadata,
            Configuration<ModelConfigOverrides_> unprocessedConfiguration, Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storeUnprocessedConfiguration(storageAddress, id, unprocessedConfiguration);
            storeModelInput(storageAddress, id, modelInput);
            storeMetadata(storageAddress, id, metadata);
            storeConfiguration(storageAddress, id, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public void storeProblem(String id, ModelInput_ modelInput, Metadata<Score_> metadata,
            Configuration<ModelConfigOverrides_> unprocessedConfiguration, Configuration<ModelConfigOverrides_> configuration) {
        storeProblem(null, id, modelInput, metadata, unprocessedConfiguration, configuration);
    }

    /**
     * Stores the outcome of a run as one unit, so that the model output, the metadata and the metrics of a data set
     * never disagree with each other for a reader.
     */
    public void storeSolution(String id, ModelOutput_ modelOutput, Metadata<Score_> metadata, InputMetrics_ inputMetrics,
            OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            storeModelOutput(id, modelOutput);
            storeMetadata(id, metadata);
            storeInputMetrics(id, inputMetrics);
            storeOutputMetrics(id, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    /**
     * Updates the outcome of a run as one unit; see {@link #storeSolution}.
     */
    public void updateSolution(String id, ModelOutput_ modelOutput, Metadata<Score_> metadata, InputMetrics_ inputMetrics,
            OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            updateModelOutput(id, modelOutput);
            updateMetadata(id, metadata);
            updateInputMetrics(id, inputMetrics);
            updateOutputMetrics(id, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public Configuration<ModelConfigOverrides_> getConfiguration(String id) {
        return getConfiguration(null, id);
    }

    public Configuration<ModelConfigOverrides_> getConfiguration(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return readSubModel(storageAddress, id, SubModelKind.CONFIG, getConfigurationClass());
        } finally {
            releaseLock(id);
        }
    }

    public Configuration<ModelConfigOverrides_> getUnprocessedConfiguration(String id) {
        return getUnprocessedConfiguration(null, id);
    }

    public Configuration<ModelConfigOverrides_> getUnprocessedConfiguration(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return readSubModel(storageAddress, id, SubModelKind.UNPROCESSED_CONFIG, getConfigurationClass());
        } finally {
            releaseLock(id);
        }
    }

    public void storeConfiguration(String id, Configuration<ModelConfigOverrides_> configuration) {
        storeConfiguration(null, id, configuration);
    }

    public void storeConfiguration(StorageAddress storageAddress, String id,
            Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public void storeUnprocessedConfiguration(String id, Configuration<ModelConfigOverrides_> configuration) {
        storeUnprocessedConfiguration(null, id, configuration);
    }

    public void storeUnprocessedConfiguration(StorageAddress storageAddress, String id,
            Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.UNPROCESSED_CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    /**
     * Reads the parts the solver starts from as one unit, so that they all come from the same state of the data set.
     */
    public SolverInput<ModelInput_, ModelOutput_, ModelConfigOverrides_> getSolverInput(String id) {
        acquireLock(id);
        try {
            ModelInput_ modelInput = getModelInput(id);
            ModelOutput_ modelOutput = getModelOutput(id);
            Configuration<ModelConfigOverrides_> configuration = getConfiguration(id);
            return new SolverInput<>(modelInput, configuration, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    public void storePatchRequest(String id, ModelInputPatchRequest<ModelConfigOverrides_> patchRequest) {
        storePatchRequest(null, id, patchRequest);
    }

    public void storePatchRequest(StorageAddress storageAddress, String id,
            ModelInputPatchRequest<ModelConfigOverrides_> patchRequest) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.PATCH_REQUEST, patchRequest);
        } finally {
            releaseLock(id);
        }
    }

    public ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> getModelResponse(String id) {
        return getModelResponse(null, id);
    }

    public ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> getModelResponse(StorageAddress storageAddress,
            String id) {
        acquireLock(id);
        try {
            Metadata<Score_> metadata = readMetadata(storageAddress, id);
            if (metadata == null) {
                throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find data set for id " + id);
            }
            try {
                ModelOutput_ modelOutput = readModelOutput(storageAddress, id);
                OutputMetrics_ outputMetrics =
                        (OutputMetrics_) readSubModel(storageAddress, id, SubModelKind.KPIS, getOutputMetricsClass());
                InputMetrics_ inputMetrics = (InputMetrics_) readSubModel(storageAddress, id, SubModelKind.INPUT_METRICS,
                        getInputMetricsClass());
                return new ModelResponse<>(metadata, modelOutput, inputMetrics, outputMetrics);
            } catch (ItemNotFoundException e) {
                // The solver pod has not created the model output yet.
                return new ModelResponse<>(metadata, null, null, null);
            }
        } finally {
            releaseLock(id);
        }
    }

    public ModelRequest<ModelInput_, ModelConfigOverrides_> getModelRequest(String id) {
        return getModelRequest(null, id);
    }

    /**
     * Reads the request the data set was created from as one unit, so that the input and the configuration come from
     * the same state of the data set.
     */
    public ModelRequest<ModelInput_, ModelConfigOverrides_> getModelRequest(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            ModelInput_ modelInput = getModelInput(storageAddress, id);
            Configuration<ModelConfigOverrides_> unprocessedConfiguration = getUnprocessedConfiguration(storageAddress, id);
            if (unprocessedConfiguration != null) {
                return new ModelRequest<>(unprocessedConfiguration, modelInput);
            }
            Configuration<ModelConfigOverrides_> configOverrides = getConfiguration(storageAddress, id);
            return new ModelRequest<>(configOverrides, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public void storeInputMetrics(String id, InputMetrics_ inputMetrics) {
        acquireLock(id);
        try {
            storeSubModel(null, id, SubModelKind.INPUT_METRICS, inputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void storeOutputMetrics(String id, OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            storeSubModel(null, id, SubModelKind.KPIS, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void updateInputMetrics(String id, InputMetrics_ inputMetrics) {
        acquireLock(id);
        try {
            updateSubModel(null, id, SubModelKind.INPUT_METRICS, inputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void updateOutputMetrics(String id, OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            updateSubModel(null, id, SubModelKind.KPIS, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public LogInfo getLogs(String id) {
        return getLogs(null, id);
    }

    public LogInfo getLogs(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return readSubModel(storageAddress, id, SubModelKind.LOGS, LogInfo.class);
        } finally {
            releaseLock(id);
        }
    }

    public void storeLogs(String id, LogInfo info) {
        storeLogs(null, id, info);
    }

    public void storeLogs(StorageAddress storageAddress, String id, LogInfo info) {
        acquireLock(id);
        try {
            storeSubModel(storageAddress, id, SubModelKind.LOGS, info);
        } finally {
            releaseLock(id);
        }
    }

    public void storeExecutionArtifacts(String id, InputStream input) {
        storeExecutionArtifacts(null, id, input);
    }

    public void storeExecutionArtifacts(StorageAddress storageAddress, String id, InputStream input) {
        acquireLock(id);
        try {
            SubModelKind kind = SubModelKind.EXECUTION_ARTIFACTS;
            StorageContent content = new StorageContent(() -> input, -1, Map.of(), false);
            writeWithRetry(id, "store the submodel (" + kind + ")", content,
                    () -> storage.storeSubModel(storageAddress, id, kind, content));
        } finally {
            releaseLock(id);
        }
    }

    public void deleteAll(String id) {
        deleteAll(null, id);
    }

    public void deleteAll(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            runWithRetry(id, "delete the dataset", () -> storage.delete(storageAddress, id));
            outputCache.remove(id);
            metadataCache.remove(id);
        } finally {
            releaseLock(id);
        }
    }

    public void restoreAll(String id) {
        restoreAll(null, id);
    }

    public void restoreAll(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            runWithRetry(id, "restore the dataset", () -> storage.restore(storageAddress, id));
        } finally {
            releaseLock(id);
        }
    }

    public boolean exists(String id) {
        return exists(null, id);
    }

    public boolean exists(StorageAddress storageAddress, String id) {
        return withRetry(id, "check whether the dataset exists", () -> storage.exists(storageAddress, id));
    }

    public boolean existsSubModel(StorageAddress options, String id, SubModelKind subModelKind) {
        return withRetry(id, "check whether the submodel (" + subModelKind + ") exists",
                () -> storage.existsSubModel(options, id, subModelKind));
    }

    public List<Metadata<Score_>> listRuns(int pageNumber, int pageSize) {
        return listRuns(null, pageNumber, pageSize);
    }

    public List<Metadata<Score_>> listRuns(StorageAddress storageAddress, int pageNumber, int pageSize) {
        List<StorageItem> items =
                withRetry(null, "list the datasets", () -> storage.list(storageAddress, pageNumber, pageSize));
        List<Metadata<Score_>> runs = new ArrayList<>(items.size());
        for (StorageItem item : items) {
            Metadata<Score_> metadata = toMetadata(storageAddress, item);
            if (metadata != null) {
                runs.add(metadata);
            }
        }
        return runs;
    }

    public <T> T getWaypoints(String id, TypeReference<T> clazz) {
        acquireLock(id);
        try {
            return readSubModel(null, id, SubModelKind.WAYPOINTS, clazz);
        } finally {
            releaseLock(id);
        }
    }

    public void storeWaypoints(String id, Object waypoints) {
        acquireLock(id);
        try {
            storeSubModel(null, id, SubModelKind.WAYPOINTS, waypoints);
        } finally {
            releaseLock(id);
        }
    }

    public <T> T getSubModel(StorageAddress options, String id, SubModelKind config, Class<T> clazz) {
        acquireLock(id);
        try {
            return readSubModel(options, id, config, clazz);
        } finally {
            releaseLock(id);
        }
    }

    public <T> T getSubModel(StorageAddress options, String id, SubModelKind config, TypeReference<T> clazz) {
        acquireLock(id);
        try {
            return readSubModel(options, id, config, clazz);
        } finally {
            releaseLock(id);
        }
    }

    /**
     * Writes the content of the sub model, as it is stored, into given output stream. The content is always written
     * compressed, regardless of how it is stored in the underlying data store.
     *
     * @param options storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param subModelKind kind of the sub model e.g. waypoints
     * @param out output stream the content should be written to
     * @throws ItemNotFoundException in case there is no such sub model
     * @implNote This is the one read that is not attempted again after a failure; bytes may already have reached the
     *           stream of the caller by then, and starting over would append the content twice.
     */
    public void getSubModelStream(StorageAddress options, String id, SubModelKind subModelKind, OutputStream out) {
        acquireLock(id);
        try (InputStream content = storage.getSubModel(options, id, subModelKind)) {
            if (content == null) {
                throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find dataset for id " + id);
            }
            CompressionUtils.transferDataCompressIfNeeded(content, out);
        } catch (IOException e) {
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_READ,
                    "Unable to read submodel (" + subModelKind + ") from the storage for id " + id, e);
        } finally {
            releaseLock(id);
        }
    }

    protected abstract Class<?> getModelInputClass();

    protected abstract Class<?> getModelOutputClass();

    protected abstract Class<?> getInputMetricsClass();

    protected abstract Class<?> getOutputMetricsClass();

    protected abstract TypeReference<Configuration<ModelConfigOverrides_>> getConfigurationClass();

    /*
     * (De)serialization of the content exchanged with the storage. None of the methods below acquires the lock, they
     * are expected to be called from methods that already hold it.
     */

    /**
     * Reads the data set from the storage and deserializes it into the model output type.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @return the model output, never null
     * @throws ItemNotFoundException in case given data set does not exist
     */
    protected ModelOutput_ readModelOutput(StorageAddress address, String id) {
        if (useCache) {
            ModelOutput_ cached = outputCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        ModelOutput_ modelOutput = (ModelOutput_) read(id, "dataset",
                () -> storage.get(address, id), (m, content) -> m.readValue(content, getModelOutputClass()));
        cacheModelOutput(id, modelOutput);
        return modelOutput;
    }

    /**
     * Reads the metadata sub model of given data set, consulting the cache first.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @return the metadata or null if there is none
     */
    protected Metadata<Score_> readMetadata(StorageAddress address, String id) {
        if (useCache) {
            Metadata<Score_> cached = metadataCache.get(id);
            if (cached != null) {
                return cached;
            }
        }
        return (Metadata<Score_>) readSubModel(address, id, SubModelKind.METADATA, Metadata.class);
    }

    /**
     * Reads given sub model from the storage and deserializes it into given type.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param clazz class the sub model should be deserialized into
     * @return the sub model or null if there is none
     */
    protected <T> T readSubModel(StorageAddress address, String id, SubModelKind kind, Class<T> clazz) {
        return read(id, "submodel (" + kind + ")", () -> storage.getSubModel(address, id, kind),
                (m, content) -> m.readValue(content, clazz));
    }

    /**
     * Reads given sub model from the storage and deserializes it into given generic type.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param typeReference type the sub model should be deserialized into
     * @return the sub model or null if there is none
     */
    protected <T> T readSubModel(StorageAddress address, String id, SubModelKind kind, TypeReference<T> typeReference) {
        return read(id, "submodel (" + kind + ")", () -> storage.getSubModel(address, id, kind),
                (m, content) -> m.readValue(content, typeReference));
    }

    /**
     * Serializes given sub model and stores it in the storage.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param subModel the sub model to be stored
     */
    protected void storeSubModel(StorageAddress address, String id, SubModelKind kind, Object subModel) {
        StorageContent content = toContent(id, subModel);
        writeWithRetry(id, "store the submodel (" + kind + ")", content,
                () -> storage.storeSubModel(address, id, kind, content));
        cacheMetadata(id, kind, subModel);
    }

    /**
     * Serializes given sub model and updates it in the storage.
     *
     * @param address storage address to apply during the operation, can be null to use the default location
     * @param id unique identifier of the data set
     * @param kind kind of the sub model e.g. waypoints
     * @param subModel the sub model to be stored
     */
    protected void updateSubModel(StorageAddress address, String id, SubModelKind kind, Object subModel) {
        StorageContent content = toContent(id, subModel);
        writeWithRetry(id, "update the submodel (" + kind + ")", content,
                () -> storage.updateSubModel(address, id, kind, content));
        cacheMetadata(id, kind, subModel);
    }

    /**
     * Serializes and compresses given value into content that can be handed over to the storage.
     *
     * @param id unique identifier of the data set the value belongs to
     * @param value the value to be serialized
     * @return content to be stored
     */
    protected StorageContent toContent(String id, Object value) {
        return StorageContent.of(CompressionUtils.compress(writeAsBytes(id, value)), attributes(value));
    }

    private byte[] writeAsBytes(String id, Object value) {
        try {
            return mapper().writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_WRITE,
                    "Unable to write dataset to the storage for id " + id, e, false);
        }
    }

    /**
     * Every read opens a new stream, so a read that failed recoverably is simply attempted again.
     */
    private <T> T read(String id, String description, ContentSupplier supplier, ContentReader<T> reader) {
        return withRetry(id, "read the " + description, () -> readOnce(id, description, supplier, reader));
    }

    private <T> T readOnce(String id, String description, ContentSupplier supplier, ContentReader<T> reader) {
        try (InputStream stored = supplier.get()) {
            if (stored == null) {
                return null;
            }
            try (InputStream content = CompressionUtils.decompressIfNeeded(stored)) {
                return reader.read(mapper(), content);
            }
        } catch (DatabindException e) {
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_READ,
                    "Unable to read " + description + " from the storage for id " + id, e, false);
        } catch (IOException e) {
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_READ,
                    "Unable to read " + description + " from the storage for id " + id, e);
        }
    }

    /**
     * Reconstructs the metadata of given item, either from the attributes of the underlying data store or, when the
     * data store does not provide them, from the content of the metadata sub model.
     */
    private Metadata<Score_> toMetadata(StorageAddress address, StorageItem item) {
        if (hasSolverStatus(item.attributes())) {
            return mapper().convertValue(item.attributes(), Metadata.class);
        }
        /*
         * The attributes were not populated when the data set was stored, fall back to reading the content. Listing
         * itself is not done under a lock, but reading and rewriting the metadata of a single data set is, so that it
         * does not interleave with whoever is writing that same data set.
         */
        acquireLock(item.id());
        try {
            Metadata<Score_> metadata = readMetadata(address, item.id());
            if (metadata == null) {
                LOGGER.debug("Unable to load run with id {}", item.id());
                return null;
            }
            if (storage.supportsAttributes()) {
                // Store it again, so the attributes get populated and the content does not have to be read next time.
                updateSubModel(address, item.id(), SubModelKind.METADATA, metadata);
            }
            return metadata;
        } finally {
            releaseLock(item.id());
        }
    }

    private static boolean hasSolverStatus(Map<String, String> attributes) {
        // Some data stores lowercase the attribute names.
        return attributes.keySet().stream().anyMatch(SOLVER_STATUS_ATTRIBUTE::equalsIgnoreCase);
    }

    private static Map<String, String> attributes(Object value) {
        return value instanceof Metadata<?> metadata ? metadata.asMap() : Map.of();
    }

    private void cacheModelOutput(String id, ModelOutput_ modelOutput) {
        if (useCache && modelOutput != null) {
            outputCache.put(id, modelOutput);
        }
    }

    private void cacheMetadata(String id, SubModelKind kind, Object subModel) {
        if (useCache && kind == SubModelKind.METADATA && subModel instanceof Metadata) {
            metadataCache.put(id, (Metadata<Score_>) subModel);
        }
    }

    protected ObjectMapper mapper() {
        if (mapper == null) {
            throw new IllegalStateException("The storage object mapper has not been set");
        }
        return mapper;
    }

    /*
     * Retrying of the operations against the storage. A data store, or the access service in front of it, can be
     * briefly unavailable; since every write is a full overwrite keyed by the id of the data set, sending it again is
     * harmless even when the failed attempt did reach the data store after all.
     */

    /**
     * Runs a read against the storage, retrying it while it keeps failing recoverably. Reads can always be attempted
     * again, they produce a new stream every time.
     */
    protected <T> T withRetry(String id, String description, Supplier<T> operation) {
        return attempt(id, description, true, operation);
    }

    /**
     * Runs an operation against the storage that has no content of its own, such as a delete or an administrative
     * one, retrying it while it keeps failing recoverably.
     */
    protected void runWithRetry(String id, String description, Runnable operation) {
        attempt(id, description, true, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Writes content to the storage, retrying it while it keeps failing recoverably. Only content that can be read
     * again is retried; a stream somebody else opened has already been consumed by the failed attempt.
     */
    protected void writeWithRetry(String id, String description, StorageContent content, Runnable operation) {
        attempt(id, description, content.repeatable(), () -> {
            operation.run();
            return null;
        });
    }

    private <T> T attempt(String id, String description, boolean repeatable, Supplier<T> operation) {
        int attempt = 1;
        Duration delay = retryInitialDelay;
        while (true) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                if (attempt >= retryMaxAttempts || !repeatable || !isRecoverable(e)) {
                    throw e;
                }
                LOGGER.warn("Attempt {} of {} to {} for id {} failed, retrying in {}: {}",
                        attempt, retryMaxAttempts, description, id, delay, e.getMessage());
                await(delay, description, id);
                delay = nextDelay(delay);
                attempt++;
            }
        }
    }

    /**
     * Only a failure the storage itself reported as recoverable is attempted again; anything else, a data set that
     * does not exist or content that cannot be deserialized included, would fail exactly the same way next time.
     */
    private static boolean isRecoverable(RuntimeException e) {
        return e instanceof TimefoldRuntimeException timefoldException && timefoldException.isRecoverable();
    }

    private Duration nextDelay(Duration delay) {
        Duration doubled = delay.multipliedBy(2);
        return doubled.compareTo(retryMaxDelay) > 0 ? retryMaxDelay : doubled;
    }

    private static void await(Duration delay, String description, String id) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN,
                    "Interrupted while waiting to " + description + " for id " + id + " again", e, true);
        }
    }

    protected void acquireLock(String id) {
        LockReference reference;
        tryAcquireLock(generalLock);
        try {
            reference = locks.computeIfAbsent(id, k -> new LockReference());
            reference.users++;
        } finally {
            generalLock.unlock();
        }

        try {
            tryAcquireLock(reference.lock);
        } catch (RuntimeException e) {
            // The lock was counted as taken before it was awaited, so the count has to be given back.
            dropReference(id);
            throw e;
        }
    }

    protected void releaseLock(String id) {
        tryAcquireLock(generalLock);

        try {
            LockReference reference = locks.get(id);
            if (reference != null && reference.lock.isHeldByCurrentThread()) {
                reference.lock.unlock();
                dropReference(id, reference);
            }
        } finally {
            generalLock.unlock();
        }
    }

    private void dropReference(String id) {
        tryAcquireLock(generalLock);
        try {
            dropReference(id, locks.get(id));
        } finally {
            generalLock.unlock();
        }
    }

    /**
     * Forgets the lock of given data set once nobody is using it any more. Must be called while holding the general
     * lock, which is also what {@link #acquireLock(String)} takes to hand the lock out, so that a lock is never
     * dropped from under a thread that is about to take it or that still holds it. The lock is reentrant, an outer
     * operation of the same thread counts as a user of its own.
     */
    private void dropReference(String id, LockReference reference) {
        if (reference == null) {
            return;
        }
        reference.users--;
        if (reference.users <= 0) {
            locks.remove(id, reference);
        }
    }

    protected void tryAcquireLock(Lock lock) {
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN, "Timeout acquiring lock to access to storage",
                        true);
            }
        } catch (InterruptedException e) {
            // The lock was never taken, there is nothing to unlock here.
            Thread.currentThread().interrupt();
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN, "Interrupted acquiring lock to access to storage",
                    e, true);
        }
    }

    /**
     * The lock of one data set together with the number of operations that are holding or awaiting it. The counter is
     * only ever touched while holding the general lock.
     */
    private static final class LockReference {

        private final ReentrantLock lock = new ReentrantLock();

        private int users;
    }

    @FunctionalInterface
    private interface ContentSupplier {

        InputStream get();
    }

    @FunctionalInterface
    private interface ContentReader<T> {

        T read(ObjectMapper mapper, InputStream content) throws IOException;
    }
}
