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

    protected SolverWorkerEvent(Metadata metadata, SolverModel model, SolverJob job, String planName, String tenantName,
            String eventProducerId) {
        super(metadata);
        this.model = model;
        this.job = job;
        this.planName = planName;
        this.tenantName = tenantName;
        this.eventProducerId = eventProducerId;
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

}
