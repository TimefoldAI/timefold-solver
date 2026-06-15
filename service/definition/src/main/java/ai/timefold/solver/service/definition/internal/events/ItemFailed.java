package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Event that indicates a dataset has failed during processing.
 */
public final class ItemFailed extends SolverWorkerEvent {

    private transient Throwable cause;

    public ItemFailed(Metadata metadata, Throwable cause, String planName, String tenantName) {
        super(metadata, null, null, planName, tenantName, null);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
