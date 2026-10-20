package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.usage.UniqueResources;

/**
 * Event carrying the {@link UniqueResources} reported for an operation, optionally tied to a dataset.
 */
public non-sealed class UniqueResourceReportEvent extends AbstractEvent {

    private final String operationId;
    private final UniqueResources uniqueResources;

    public UniqueResourceReportEvent(String operationId, UniqueResources uniqueResources) {
        super(null);
        this.operationId = operationId;
        this.uniqueResources = uniqueResources;
    }

    public UniqueResourceReportEvent(String operationId, String datasetId, UniqueResources uniqueResources) {
        super(datasetId);
        this.operationId = operationId;
        this.uniqueResources = uniqueResources;
    }

    public String getOperationId() {
        return operationId;
    }

    public UniqueResources getUniqueResources() {
        return uniqueResources;
    }
}
