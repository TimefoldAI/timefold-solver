package ai.timefold.solver.service.definition.impl.solver;

import java.util.List;
import java.util.Set;

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

/**
 * Facade used by the REST layer to interact with dataset storage and the solver worker,
 * without depending on storage or messaging internals directly.
 */
public interface SolverWorkerFacade {

    /***
     * Starts solving an existing dataset identified by the given id.
     * 
     * @param id the dataset identifier
     * @return the {@link Metadata} associated with the dataset that will be solved
     * @throws ai.timefold.solver.service.definition.internal.error.ItemNotFoundException
     *         if no dataset exists for the given id
     */
    <Score_> Metadata<Score_> solveDataset(String id);

    /**
     * Creates a new dataset derived from an existing one (identified by {@code id}), * without starting the solving process.
     * The resulting dataset is validated and * computed asynchronously.
     * 
     * @param id the identifier of the source (parent)dataset.
     * @param select whether to derive from the unsolved input or the solved output of the parent
     * @param runName a human-readable name for the new run * @param tags the set of tags to attach to the new dataset
     * @param configuration the configuration to apply; if {@code null}, the parent's configuration is reused
     * @return the {@link Metadata} of the newly created dataset
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createDataset(String id,
            DatasetSelector select, String runName, Set<String> tags,
            Configuration<ModelConfigurationOverrides_> configuration);

    /**
     * Creates a new dataset derived from an existing one (identified by {@code id}) and immediately schedules it to be
     * solved after validation/compute completes.
     * 
     * @param id the identifier of the source (parent) dataset
     * @param select whether to derive from the unsolved input or the solved output of the parent
     * @param runName a human-readable name for the new run
     * @param tags the set of tags to attach to the new dataset
     * @param configuration the configuration to apply; if {@code null}, the parent's configuration is reused
     * @return the {@link Metadata} of the newly created dataset
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createAndSolveDataset(String id,
            DatasetSelector select, String runName, Set<String> tags,
            Configuration<ModelConfigurationOverrides_> configuration);

    /**
     * Creates a new dataset from the provided {@link ModelInput} without starting the solving process. The resulting
     * dataset is validated and computed asynchronously.
     * 
     * @param runName a human-readable name for the new run
     * @param tags the set of tags to attach to the new dataset
     * @param modelInput the model input to store as the dataset problem
     * @param configuration the configuration to apply to the dataset
     * @return the {@link Metadata} of the newly created dataset
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createDataset(String runName,
            Set<String> tags, ModelInput modelInput, Configuration<ModelConfigurationOverrides_> configuration);

    /**
     * Creates a new dataset from the provided {@link ModelInput} and immediately schedules it to be solved after
     * validation/compute completes.
     * 
     * @param runName a human-readable name for the new run
     * @param tags the set of tags to attach to the new dataset
     * @param modelInput the model input to store as the dataset problem
     * @param configuration the configuration to apply to the dataset
     * @return the {@link Metadata} of the newly created dataset
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> createAndSolveDataset(String runName,
            Set<String> tags, ModelInput modelInput,
            Configuration<ModelConfigurationOverrides_> configuration);

    /**
     * Creates a new dataset by applying a JSON patch to an existing dataset's {@link ModelInput}, without starting
     * the solving process. The resulting dataset is validated and computed asynchronously.
     *
     * @param id the identifier of the source (parent) dataset
     * @param select whether to derive from the unsolved input or the solved output of the parent
     * @param runName a human-readable name for the new run
     * @param modelInputPatchRequest the patch request containing the JSON patch operations and an optional
     *        {@link Configuration} override
     * @return the {@link Metadata} of the newly created dataset
     * @throws ai.timefold.solver.service.definition.internal.error.ItemNotFoundException
     *         if no dataset exists for the given id
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> patchDataset(String id,
            DatasetSelector select, String runName,
            ModelInputPatchRequest<ModelConfigurationOverrides_> modelInputPatchRequest);

    /**
     * Creates a new dataset by applying a JSON patch to an existing dataset's {@link ModelInput} and immediately
     * schedules it to be solved after validation/compute completes.
     *
     * @param id the identifier of the source (parent) dataset
     * @param select whether to derive from the unsolved input or the solved output of the parent
     * @param runName a human-readable name for the new run
     * @param modelInputPatchRequest the patch request containing the JSON patch operations and an optional
     *        {@link Configuration} override
     * @return the {@link Metadata} of the newly created dataset
     * @throws ai.timefold.solver.service.definition.internal.error.ItemNotFoundException
     *         if no dataset exists for the given id
     */
    <Score_, ModelConfigurationOverrides_ extends ModelConfigOverrides> Metadata<Score_> patchAndSolveDataset(String id,
            DatasetSelector select, String runName,
            ModelInputPatchRequest<ModelConfigurationOverrides_> modelInputPatchRequest);

    /**
     * Terminates the ongoing solve for the given dataset (if any) and returns the current model response
     * 
     * @param id the dataset identifier * @return the {@link ModelResponse} for the dataset after termination
     */
    <Score_, ModelOutput_ extends ModelOutput, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics>
            ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> terminate(String id);

    /**
     * Returns the {@link Metadata} for the given dataset, or {@code null} if none exists.
     * 
     * @param id the dataset identifier
     * @return the dataset metadata, or {@code null} if not found
     */
    <Score_> Metadata<Score_> getMetadata(String id);

    /**
     * Returns the effective (processed) {@link Configuration} of the given dataset.
     * 
     * @param id the dataset identifier
     * @return the effective configuration, or {@code null} if not found
     */
    <ModelConfigurationOverrides_ extends ModelConfigOverrides> Configuration<ModelConfigurationOverrides_>
            getConfiguration(String id);

    /**
     * Returns the original (unprocessed) {@link Configuration} of the given dataset, as it was supplied by the caller
     * before any post-processing.
     * 
     * @param id the dataset identifier
     * @return the unprocessed configuration, or {@code null}if not found
     */
    <ModelConfigurationOverrides_ extends ModelConfigOverrides> Configuration<ModelConfigurationOverrides_>
            getUnprocessedConfiguration(String id);

    /**
     * * Returns the {@link ModelInput} originally submitted for the given dataset. * * @param id the dataset identifier
     * * @return the model input, or {@code null} if not found
     */
    ModelInput getModelInput(String id);

    /**
     * Returns the {@link ModelInput} representing the solved state of the given dataset (i.e. planning entities updated
     * with the best solution).
     * 
     * @param id the dataset identifier
     * @return the solved model input, or {@code null} if notavailable
     */
    ModelInput getSolvedModelInput(String id);

    /**
     * Returns a paginated list of dataset runs.
     * 
     * @param pageNumber the zero-based page index
     * @param pageSize the number of runs per page
     * @return the list of {@link Metadata} entries for the requested page
     */
    <Score_> List<Metadata<Score_>> listRuns(int pageNumber, int pageSize);

    /**
     * Returns the {@link ModelResponse} for the given dataset.
     * 
     * @param id the dataset identifier
     * @return the model response, or {@code null} if not available
     */
    <Score_, ModelOutput_ extends ModelOutput, InputMetrics_ extends ModelInputMetrics, OutputMetrics_ extends ModelOutputMetrics>
            ModelResponse<Score_, ModelOutput_, InputMetrics_, OutputMetrics_> getModelResponse(String id);

    /**
     * Returns the original {@link ModelRequest} for the given dataset.
     * 
     * @param id the dataset identifier
     * @return the model request, or {@code null} if not found
     */
    <ModelInput_ extends ModelInput, ModelConfigurationOverrides_ extends ModelConfigOverrides>
            ModelRequest<ModelInput_, ModelConfigurationOverrides_> getModelRequest(String id);

    /**
     * Returns the solver logs associated with the given dataset.
     * The result combines the currently running pod's log file (if present) with any previously persisted log content
     * 
     * @param id the dataset identifier
     * @return the aggregated {@link LogInfo}, or {@code null} if no logs are available
     */
    LogInfo getLogs(String id);

}
