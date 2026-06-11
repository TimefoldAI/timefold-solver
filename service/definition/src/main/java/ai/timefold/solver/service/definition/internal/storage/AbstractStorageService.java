package ai.timefold.solver.service.definition.internal.storage;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import ai.timefold.solver.service.definition.impl.validation.JsonMappingError;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

public abstract non-sealed class AbstractStorageService<ModelInput_ extends ModelInput, ModelConfigOverrides_ extends ModelConfigOverrides, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics, ModelOutput_ extends ModelOutput, Score_, Justification_ extends ModelConstraintJustification>
        implements StorageServiceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageService.class);

    private static final long LOCK_TIMEOUT_SECONDS = 60;

    protected Storage<ModelOutput_> storage;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Lock generalLock = new ReentrantLock();

    @SuppressWarnings("unused")
    protected AbstractStorageService() {
        /* Mandatory for recording */
    }

    // For tests
    protected AbstractStorageService(Storage<ModelOutput_> storage) {
        this.storage = storage;
    }

    @Inject
    protected void setStorage(Instance<Storage<ModelOutput_>> instance) {
        storage = instance.get();
    }

    public void create(String id, StorageConfiguration storageConfiguration) {
        acquireLock(id);
        try {
            this.storage.create(id, storageConfiguration);
        } finally {
            releaseLock(id);
        }
    }

    public void reconfigure(String id, StorageConfiguration storageConfiguration) {
        acquireLock(id);
        try {
            this.storage.reconfigure(id, storageConfiguration);
        } finally {
            releaseLock(id);
        }
    }

    public void destroy(String id) {
        acquireLock(id);
        try {
            this.storage.destroy(id);
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
            return (ModelInput_) storage.getSubModel(storageAddress, id, SubModelKind.MODEL_INPUT, getModelInputClass());
        } catch (TimefoldRuntimeException timefoldRuntimeException) {
            if (timefoldRuntimeException.getCause() instanceof JsonMappingException mappingException) {
                var validationBuilder = new ValidationBuilder().addIssue(new JsonMappingError(mappingException.getMessage()));

                LegacyValidationResult legacyValidationResult = validationBuilder.buildLegacyValidationResult();
                Metadata<Score_> metadata = (Metadata<Score_>) storage.getSubModel(id, SubModelKind.METADATA, Metadata.class);
                if (metadata != null) {
                    metadata.datasetValidated(legacyValidationResult);
                    storage.updateSubModel(storageAddress, id, SubModelKind.METADATA, metadata); // For backward compatibility.
                    storage.storeSubModel(storageAddress, id, SubModelKind.VALIDATION_RESULT, validationBuilder.build());
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
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.MODEL_INPUT, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public void storeModelInput(StorageAddress storageAddress, String id, ModelInput_ modelInput) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.MODEL_INPUT, modelInput);
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
            return (ModelInput_) storage.getSubModel(storageAddress, id, SubModelKind.MODEL_INPUT_SOLVED, getModelInputClass());
        } finally {
            releaseLock(id);
        }
    }

    public void storeSolvedModelInput(String id, ModelInput_ modelInput) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.MODEL_INPUT_SOLVED, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public void storeSolvedModelInput(StorageAddress storageAddress, String id, ModelInput_ modelInput) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.MODEL_INPUT_SOLVED, modelInput);
        } finally {
            releaseLock(id);
        }
    }

    public Metadata<Score_> getMetadata(String id) {
        acquireLock(id);
        try {
            return (Metadata<Score_>) storage.getSubModel(id, SubModelKind.METADATA, Metadata.class);
        } finally {
            releaseLock(id);
        }
    }

    public Metadata<Score_> getMetadata(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return (Metadata<Score_>) storage.getSubModel(storageAddress, id, SubModelKind.METADATA, Metadata.class);
        } finally {
            releaseLock(id);
        }
    }

    public void storeMetadata(String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public void storeMetadata(StorageAddress storageAddress, String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public void updateMetadata(String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            storage.updateSubModel(id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public void updateMetadata(StorageAddress storageAddress, String id, Metadata<Score_> metadata) {
        acquireLock(id);
        try {
            storage.updateSubModel(storageAddress, id, SubModelKind.METADATA, metadata);
        } finally {
            releaseLock(id);
        }
    }

    public <T extends Issue> void storeValidationResponse(StorageAddress storageAddress, String id,
            ValidationResult<T> validationResult) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.VALIDATION_RESULT, validationResult);
        } finally {
            releaseLock(id);
        }
    }

    public <T extends Issue> void storeValidationResponse(String id, ValidationResult<T> validationResult) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.VALIDATION_RESULT, validationResult);
        } finally {
            releaseLock(id);
        }
    }

    public ModelOutput_ getModelOutput(String id) {
        acquireLock(id);
        try {
            return storage.get(id);
        } finally {
            releaseLock(id);
        }
    }

    public void storeModelOutput(String id, ModelOutput_ modelOutput) {
        acquireLock(id);
        try {
            storage.store(id, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    public void updateModelOutput(String id, ModelOutput_ modelOutput) {
        acquireLock(id);
        try {
            storage.update(id, modelOutput);
        } finally {
            releaseLock(id);
        }
    }

    public void storeProblem(StorageAddress storageAddress, String id, ModelInput_ modelInput, Metadata<Score_> metadata,
            Configuration<ModelConfigOverrides_> unprocessedConfiguration, Configuration<ModelConfigOverrides_> configuration) {
        storeUnprocessedConfiguration(storageAddress, id, unprocessedConfiguration);
        storeModelInput(storageAddress, id, modelInput);
        storeMetadata(storageAddress, id, metadata);
        storeConfiguration(storageAddress, id, configuration);
    }

    public void storeProblem(String id, ModelInput_ modelInput, Metadata<Score_> metadata,
            Configuration<ModelConfigOverrides_> unprocessedConfiguration, Configuration<ModelConfigOverrides_> configuration) {
        storeUnprocessedConfiguration(id, unprocessedConfiguration);
        storeModelInput(id, modelInput);
        storeMetadata(id, metadata);
        storeConfiguration(id, configuration);
    }

    public void storeSolution(String id, ModelOutput_ modelOutput, Metadata<Score_> metadata, InputMetrics_ inputMetrics,
            OutputMetrics_ outputMetrics) {
        storeModelOutput(id, modelOutput);
        storeMetadata(id, metadata);
        storeInputMetrics(id, inputMetrics);
        storeOutputMetrics(id, outputMetrics);
    }

    public void updateSolution(String id, ModelOutput_ modelOutput, Metadata<Score_> metadata, InputMetrics_ inputMetrics,
            OutputMetrics_ outputMetrics) {
        updateModelOutput(id, modelOutput);
        updateMetadata(id, metadata);
        updateInputMetrics(id, inputMetrics);
        updateOutputMetrics(id, outputMetrics);
    }

    public Configuration<ModelConfigOverrides_> getConfiguration(String id) {
        return getConfiguration(null, id);
    }

    public Configuration<ModelConfigOverrides_> getConfiguration(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return storage.getSubModel(storageAddress, id, SubModelKind.CONFIG, getConfigurationClass());
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
            return storage.getSubModel(storageAddress, id, SubModelKind.UNPROCESSED_CONFIG, getConfigurationClass());
        } finally {
            releaseLock(id);
        }
    }

    public void storeConfiguration(String id, Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public void storeConfiguration(StorageAddress storageAddress, String id,
            Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public void storeUnprocessedConfiguration(String id, Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.UNPROCESSED_CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public void storeUnprocessedConfiguration(StorageAddress storageAddress, String id,
            Configuration<ModelConfigOverrides_> configuration) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.UNPROCESSED_CONFIG, configuration);
        } finally {
            releaseLock(id);
        }
    }

    public SolverInput<ModelInput_, ModelOutput_, ModelConfigOverrides_> getSolverInput(String id) {
        ModelInput_ modelInput = getModelInput(id);
        ModelOutput_ modelOutput = getModelOutput(id);
        Configuration<ModelConfigOverrides_> configuration = getConfiguration(id);
        return new SolverInput<>(modelInput, configuration, modelOutput);
    }

    public void storePatchRequest(String id, ModelInputPatchRequest<ModelConfigOverrides_> patchRequest) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.PATCH_REQUEST, patchRequest);
        } finally {
            releaseLock(id);
        }
    }

    public void storePatchRequest(StorageAddress storageAddress, String id,
            ModelInputPatchRequest<ModelConfigOverrides_> patchRequest) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.PATCH_REQUEST, patchRequest);
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
            Metadata<Score_> metadata = storage.getSubModel(storageAddress, id, SubModelKind.METADATA, Metadata.class);
            if (metadata == null) {
                throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Unable to find data set for id " + id);
            }
            try {
                ModelOutput_ modelOutput = storage.get(storageAddress, id);
                OutputMetrics_ outputMetrics =
                        (OutputMetrics_) storage.getSubModel(storageAddress, id, SubModelKind.KPIS, getOutputMetricsClass());
                InputMetrics_ inputMetrics = (InputMetrics_) storage.getSubModel(storageAddress, id, SubModelKind.INPUT_METRICS,
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

    public ModelRequest<ModelInput_, ModelConfigOverrides_> getModelRequest(StorageAddress storageAddress, String id) {
        ModelInput_ modelInput = getModelInput(storageAddress, id);
        Configuration<ModelConfigOverrides_> unprocessedConfiguration = getUnprocessedConfiguration(storageAddress, id);
        if (unprocessedConfiguration != null) {
            return new ModelRequest<>(unprocessedConfiguration, modelInput);
        }
        Configuration<ModelConfigOverrides_> configOverrides = getConfiguration(storageAddress, id);
        return new ModelRequest<>(configOverrides, modelInput);
    }

    public void storeInputMetrics(String id, InputMetrics_ inputMetrics) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.INPUT_METRICS, inputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void storeOutputMetrics(String id, OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.KPIS, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void updateInputMetrics(String id, InputMetrics_ inputMetrics) {
        acquireLock(id);
        try {
            storage.updateSubModel(id, SubModelKind.INPUT_METRICS, inputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public void updateOutputMetrics(String id, OutputMetrics_ outputMetrics) {
        acquireLock(id);
        try {
            storage.updateSubModel(id, SubModelKind.KPIS, outputMetrics);
        } finally {
            releaseLock(id);
        }
    }

    public LogInfo getLogs(String id) {
        acquireLock(id);
        try {
            return storage.getSubModel(id, SubModelKind.LOGS, LogInfo.class);
        } finally {
            releaseLock(id);
        }
    }

    public LogInfo getLogs(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            return storage.getSubModel(storageAddress, id, SubModelKind.LOGS, LogInfo.class);
        } finally {
            releaseLock(id);
        }
    }

    public void storeLogs(String id, LogInfo info) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.LOGS, info);
        } finally {
            releaseLock(id);
        }
    }

    public void storeLogs(StorageAddress storageAddress, String id, LogInfo info) {
        acquireLock(id);
        try {
            storage.storeSubModel(storageAddress, id, SubModelKind.LOGS, info);
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
            storage.delete(storageAddress, id);
        } finally {
            releaseLock(id);
        }
    }

    protected abstract Class<?> getModelInputClass();

    protected abstract Class<?> getInputMetricsClass();

    protected abstract Class<?> getOutputMetricsClass();

    protected abstract TypeReference<Configuration<ModelConfigOverrides_>> getConfigurationClass();

    public List<Metadata<Score_>> listRuns(int pageNumber, int pageSize) {
        return storage.list(pageNumber, pageSize);
    }

    public List<Metadata<Score_>> listRuns(StorageAddress storageAddress, int pageNumber, int pageSize) {
        return storage.list(storageAddress, pageNumber, pageSize);
    }

    public <T> T getWaypoints(String id, TypeReference<T> clazz) {
        acquireLock(id);
        try {
            return storage.getSubModel(id, SubModelKind.WAYPOINTS, clazz);
        } finally {
            releaseLock(id);
        }
    }

    public void storeWaypoints(String id, Object waypoints) {
        acquireLock(id);
        try {
            storage.storeSubModel(id, SubModelKind.WAYPOINTS, waypoints);
        } finally {
            releaseLock(id);
        }
    }

    public <T> T getSubModel(StorageAddress options, String id, SubModelKind config, Class<T> clazz) {
        acquireLock(id);
        try {
            return storage.getSubModel(options, id, config, clazz);
        } finally {
            releaseLock(id);
        }
    }

    protected void acquireLock(String id) {
        ReentrantLock idLock;
        tryAcquireLock(generalLock);
        try {
            idLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
        } finally {
            generalLock.unlock();
        }

        tryAcquireLock(idLock);
    }

    protected void releaseLock(String id) {
        tryAcquireLock(generalLock);

        try {
            ReentrantLock idLock = locks.get(id);
            if (idLock != null && idLock.isHeldByCurrentThread()) {
                idLock.unlock();
                if (idLock.getQueueLength() == 0) {
                    locks.remove(id, idLock);
                }
            }
        } finally {
            generalLock.unlock();
        }
    }

    protected void tryAcquireLock(Lock lock) {
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN, "Timeout acquiring lock to access to storage",
                        true);
            }
        } catch (InterruptedException e) {
            lock.unlock();
            Thread.currentThread().interrupt();
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN, "Timeout acquiring lock to access to storage", e,
                    true);
        }
    }

    public void getSubModelStream(StorageAddress options, String id, SubModelKind subModelKind, OutputStream out) {
        acquireLock(id);
        try {
            storage.getSubModelStream(options, id, subModelKind, out);
        } finally {
            releaseLock(id);
        }
    }

    public boolean existsSubModel(StorageAddress options, String id, SubModelKind subModelKind) {
        return storage.existsSubModel(options, id, subModelKind);
    }

    public void restoreAll(String id) {
        deleteAll(null, id);
    }

    public void restoreAll(StorageAddress storageAddress, String id) {
        acquireLock(id);
        try {
            storage.restore(storageAddress, id);
        } finally {
            releaseLock(id);
        }
    }
}
