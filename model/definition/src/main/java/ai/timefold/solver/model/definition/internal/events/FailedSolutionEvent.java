package ai.timefold.solver.model.definition.internal.events;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.model.definition.api.domain.Metadata;

/**
 * Sent when a solver worker fails during solving.
 */
public final class FailedSolutionEvent extends SolverWorkerEvent {

    private final transient Throwable cause;

    public FailedSolutionEvent(Metadata metadata, SolverJob job, Throwable cause, String planName, String tenantName) {
        super(metadata, null, job, planName, tenantName, null);
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "FailedSolutionEvent{" +
                "cause=" + cause +
                "} " + super.toString();
    }
}
