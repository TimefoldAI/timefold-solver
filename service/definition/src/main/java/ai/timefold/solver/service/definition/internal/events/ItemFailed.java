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

    private ItemFailed(String id, Throwable cause, String planName, String tenantName) {
        super(id, planName, tenantName);
        this.cause = cause;
    }

    /**
     * Creates the event for a run whose metadata could not be read; {@link #getMetadata()} then returns null.
     *
     * @param id the id of the failed run
     * @param cause the failure
     * @param planName the name of the plan
     * @param tenantName the name of the tenant
     * @return the event, without metadata
     */
    public static ItemFailed withoutMetadata(String id, Throwable cause, String planName, String tenantName) {
        return new ItemFailed(id, cause, planName, tenantName);
    }

    public Throwable getCause() {
        return cause;
    }
}
