package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Event sent when the outputs of a dataset are computed before solving.
 * This happens after the {@link DatasetValidatedEvent} and before the {@link SolveStartCommand}.
 */
public final class DatasetComputedEvent extends SolverWorkerEvent {

    /**
     * Indicates whether a solving operation was requested.
     * When {@code true}, computation is followed by solving, which progresses the dataset to the next state.
     * When {@code false}, {@code SolvingStatus.DATASET_COMPUTED} is the final state and no solving takes place.
     */
    private final boolean solveRequested;

    /**
     * The effective map location resolved during enrichment (e.g. the concrete region chosen by auto-select),
     * or {@code null} for models that are not map-enriched. This is the single point in a run where the
     * resolved location is reported; later events do not carry it.
     */
    private final String resolvedMapLocation;

    /**
     * The user-configured resources for this run, read from the run configuration: the number of cores (thread count)
     * and the memory in mebibytes (Mi).
     */
    private final Integer configuredCores;

    private final Integer configuredMemory;

    public DatasetComputedEvent(Metadata metadata, SolverModel solverModel, String planName, String tenantName,
            boolean solveRequested, String resolvedMapLocation, Integer configuredCores, Integer configuredMemory) {
        super(metadata, solverModel, null, planName, tenantName, null);
        this.solveRequested = solveRequested;
        this.resolvedMapLocation = resolvedMapLocation;
        this.configuredCores = configuredCores;
        this.configuredMemory = configuredMemory;
    }

    public boolean isSolveRequested() {
        return solveRequested;
    }

    public String getResolvedMapLocation() {
        return resolvedMapLocation;
    }

    public Integer getConfiguredCores() {
        return configuredCores;
    }

    public Integer getConfiguredMemory() {
        return configuredMemory;
    }
}
