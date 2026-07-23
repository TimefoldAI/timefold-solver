package ai.timefold.solver.service.worker.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelInputPatchRequest;
import ai.timefold.solver.service.definition.api.domain.ModelRequest;
import ai.timefold.solver.service.definition.api.domain.ModelResponse;
import ai.timefold.solver.service.definition.api.log.LogInfo;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.api.rest.DatasetSelector;
import ai.timefold.solver.service.definition.impl.log.LoggingConstants;
import ai.timefold.solver.service.definition.impl.solver.SolverWorkerFacade;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.service.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.service.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.service.definition.internal.events.SolverChannels;
import ai.timefold.solver.service.definition.internal.storage.AbstractStorageService;
import ai.timefold.solver.service.json.internal.patch.JsonPatch;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Broadcast;

/**
 * Single implementation of {@link SolverWorkerFacade}, orchestrating {@link AbstractStorageService} reads/writes
 * together with the dataset lifecycle commands/events the solver worker reacts to.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@ApplicationScoped
public class DefaultSolverWorkerFacade implements SolverWorkerFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolverWorkerFacade.class);

    private final AbstractStorageService storageService;

    private final Emitter<DatasetCreatedEvent> datasetCreatedEventEmitter;

    private final Emitter<DatasetValidateComputeCommand> datasetValidateComputeCommandEmitter;

    private final Emitter<SolveStartCommand> solveStartCommandEmitter;

    private final MutinyEmitter<SolveTerminateCommand> solveTerminateCommandEmitter;

    private final ObjectMapper mapper;

    @Inject
    public DefaultSolverWorkerFacade(
            @SuppressWarnings("CdiInjectionPointsInspection") AbstractStorageService<?, ?, ?, ?, ?, ?, ?> storageService,
            @Broadcast @Channel(SolverChannels.DATASET_CREATED) Emitter<DatasetCreatedEvent> datasetCreatedEventEmitter,
            @Broadcast @Channel(SolverChannels.DATASET_VALIDATE_COMPUTE) Emitter<DatasetValidateComputeCommand> datasetValidateComputeCommandEmitter,
            @Channel(SolverChannels.START) Emitter<SolveStartCommand> solveStartCommandEmitter,
            @Channel(SolverChannels.TERMINATE) MutinyEmitter<SolveTerminateCommand> solveTerminateCommandEmitter,
            ObjectMapper mapper) {
        this.storageService = storageService;
        this.datasetCreatedEventEmitter = datasetCreatedEventEmitter;
        this.datasetValidateComputeCommandEmitter = datasetValidateComputeCommandEmitter;
        this.solveStartCommandEmitter = solveStartCommandEmitter;
        this.solveTerminateCommandEmitter = solveTerminateCommandEmitter;
        this.mapper = mapper;
    }

    @Override
    public <Score_> Metadata<Score_> solveDataset(String id) {
        Metadata metadata = storageService.getMetadata(id);
        if (metadata == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "Dataset with id " + id + " was not found");
        }
        solveStartCommandEmitter.send(new SolveStartCommand(id));
        return metadata;
    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createDataset(String id,
            DatasetSelector select, String runName, Set<String> tags,
            Configuration<ModelConfigurationOverrides_> configuration) {
        var metadata = createDatasetInternal(id, select, runName, tags, configuration);
        datasetValidateComputeCommandEmitter
                .send(new DatasetValidateComputeCommand(metadata.getId(), false));
        return metadata;

    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createAndSolveDataset(String id,
            DatasetSelector select, String runName, Set<String> tags,
            Configuration<ModelConfigurationOverrides_> configuration) {
        var metadata = createDatasetInternal(id, select, runName, tags, configuration);
        datasetValidateComputeCommandEmitter
                .send(new DatasetValidateComputeCommand(metadata.getId(), true));
        return metadata;
    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createDataset(String runName,
            Set<String> tags, ModelInput modelInput, Configuration<ModelConfigurationOverrides_> configuration) {
        var metadata = createDatasetInternal(runName, tags, modelInput, configuration);
        datasetValidateComputeCommandEmitter.send(new DatasetValidateComputeCommand(metadata.getId(), false));
        return metadata;
    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createAndSolveDataset(
            String runName, Set<String> tags, ModelInput modelInput,
            Configuration<ModelConfigurationOverrides_> configuration) {
        var metadata = createDatasetInternal(runName, tags, modelInput, configuration);
        datasetValidateComputeCommandEmitter.send(new DatasetValidateComputeCommand(metadata.getId(), true));
        return metadata;
    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> patchDataset(String id,
            DatasetSelector select, String runName,
            ModelInputPatchRequest<ModelConfigurationOverrides_> modelInputPatchRequest) {
        var metadata = createPatchedDataset(id, select, runName, modelInputPatchRequest);
        datasetValidateComputeCommandEmitter.send(new DatasetValidateComputeCommand(metadata.getId(), false));
        return metadata;
    }

    @Override
    public <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> patchAndSolveDataset(String id,
            DatasetSelector select, String runName,
            ModelInputPatchRequest<ModelConfigurationOverrides_> modelInputPatchRequest) {
        var metadata = createPatchedDataset(id, select, runName, modelInputPatchRequest);
        datasetValidateComputeCommandEmitter.send(new DatasetValidateComputeCommand(metadata.getId(), true));
        return metadata;
    }

    @Override
    public <Score_, ModelOutput_ extends ModelOutput, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics>
            ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> terminate(String id) {
        solveTerminateCommandEmitter.sendAndAwait(new SolveTerminateCommand(id));
        return storageService.getModelResponse(id);
    }

    @Override
    public <Score_> Metadata<Score_> getMetadata(String id) {
        return storageService.getMetadata(id);
    }

    @Override
    public Configuration getConfiguration(String id) {
        return storageService.getConfiguration(id);
    }

    @Override
    public Configuration getUnprocessedConfiguration(String id) {
        return storageService.getUnprocessedConfiguration(id);
    }

    @Override
    public ModelInput getModelInput(String id) {
        return storageService.getModelInput(id);
    }

    @Override
    public ModelInput getSolvedModelInput(String id) {
        return storageService.getSolvedModelInput(id);
    }

    @Override
    public <Score_> List<Metadata<Score_>> listRuns(int pageNumber, int pageSize) {
        return storageService.listRuns(pageNumber, pageSize);
    }

    @Override
    public <Score_, ModelOutput_ extends ModelOutput, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics>
            ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> getModelResponse(String id) {
        return storageService.getModelResponse(id);
    }

    @Override
    public <ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides>
            ModelRequest<ModelInput_, ModelConfigurationOverrides_> getModelRequest(String id) {
        return storageService.getModelRequest(id);
    }

    @Override
    public LogInfo getLogs(String id) {
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

    private Metadata createDatasetInternal(String runName, Set<String> tags, ModelInput modelInput,
            Configuration configuration) {
        var metadata = new Metadata<>(runName);
        metadata.setTags(tags);
        metadata.datasetCreated();
        storageService.storeProblem(metadata.getId(), modelInput, metadata, configuration, configuration);
        datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
        return metadata;
    }

    private Metadata createDatasetInternal(String id, DatasetSelector select, String runName, Set<String> tags,
            Configuration configuration) {
        var modelInput =
                select == DatasetSelector.UNSOLVED ? getModelInput(id) : getSolvedModelInput(id);
        if (modelInput == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }

        Configuration initialConfiguration = null;
        if (configuration != null && configuration.run() != null) {
            initialConfiguration = configuration;
        } else if (configuration == null) {
            configuration = getConfiguration(id);
            initialConfiguration = getUnprocessedConfiguration(id);
        }

        var metadata = new Metadata<>(runName);
        metadata.setTags(tags);
        metadata.datasetCreated();
        // set parent id to the id this data set is created from
        metadata.setParentId(id);
        // set origin id based on the origin id of the parent data set
        Metadata parentRun = getMetadata(id);
        metadata.setOriginId(parentRun.getOriginId());

        storageService.storeProblem(metadata.getId(), modelInput, metadata, initialConfiguration, configuration);
        datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
        return metadata;
    }

    private Metadata createPatchedDataset(String id, DatasetSelector select, String runName,
            ModelInputPatchRequest patchRequest) {
        var input =
                select == DatasetSelector.UNSOLVED ? getModelInput(id) : getSolvedModelInput(id);
        if (input == null) {
            throw new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, id);
        }
        try {
            Configuration configuration =
                    patchRequest.config() != null ? patchRequest.config() : getConfiguration(id);
            Configuration initialConfiguration = getUnprocessedConfiguration(id);
            if (configuration == null) {
                configuration = Configuration.empty();
                initialConfiguration = Configuration.empty();
            }
            JsonNode inputTree = mapper.valueToTree(input);
            ArrayNode patchTree = mapper.valueToTree(patchRequest.patch());

            JsonNode patchedInput = JsonPatch.apply(patchTree, inputTree);

            ModelInput patchedModelInput = mapper.treeToValue(patchedInput, input.getClass());

            Metadata metadata = new Metadata<>(runName);
            // set parent id to the id this data set is created from
            metadata.setParentId(id);
            // set origin id based on the origin id of the parent data set
            Metadata parentRun = getMetadata(id);
            metadata.setOriginId(parentRun.getOriginId());
            metadata.datasetCreated();
            storageService.storeProblem(metadata.getId(), patchedModelInput, metadata, initialConfiguration, configuration);
            datasetCreatedEventEmitter.send(new DatasetCreatedEvent(metadata));
            return metadata;
        } catch (IllegalArgumentException e) { // thrown by the JsonPatch
            throw e;
        } catch (Exception e) {
            throw new TimefoldRuntimeException(ErrorCodes.UNKNOWN, UUID.randomUUID().toString(), e);
        }
    }
}
