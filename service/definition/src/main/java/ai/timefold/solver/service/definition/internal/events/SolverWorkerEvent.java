package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Base class for events produced by the SolverWorker.
 */
public abstract sealed class SolverWorkerEvent extends AbstractDatasetEvent permits DatasetComputedEvent, ItemStarted,
        InitSolutionEvent, BestSolutionEvent, FinalBestSolutionEvent, FailedSolutionEvent, ItemFailed {

    private final SolverModel model;

    private final SolverJob job;

    private final String planName;

    private final String tenantName;

    private final String eventProducerId;

    private final Integer configuredCores;

    private final Long configuredMemoryMi;

    protected SolverWorkerEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            String eventProducerId) {
        this(metadata, model, job, planName, tenantName, eventProducerId, null, null);
    }

    protected SolverWorkerEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            String eventProducerId, Integer configuredCores, Long configuredMemoryMi) {
        super(metadata);
        this.model = model;
        this.job = job;
        this.planName = planName;
        this.tenantName = tenantName;
        this.eventProducerId = eventProducerId;
        this.configuredCores = configuredCores;
        this.configuredMemoryMi = configuredMemoryMi;
    }

    public SolverModel getModel() {
        return model;
    }

    public SolverJob getJob() {
        return job;
    }

    public String getPlanName() {
        return planName;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getEventProducerId() {
        return eventProducerId;
    }

    public Integer getConfiguredCores() {
        return configuredCores;
    }

    public Long getConfiguredMemoryMi() {
        return configuredMemoryMi;
    }

}
