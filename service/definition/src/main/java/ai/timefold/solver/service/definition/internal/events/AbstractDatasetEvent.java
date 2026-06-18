package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Event referencing the associated {@link Metadata}.
 */
public abstract sealed class AbstractDatasetEvent extends AbstractEvent
        permits DatasetCreatedEvent, DatasetValidatedEvent, SolverWorkerEvent {

    private final Metadata metadata;

    private String resolvedMapLocation;

    protected AbstractDatasetEvent(Metadata metadata) {
        super(metadata.getId());
        // Safe copy of the run to avoid external modifications.
        this.metadata = new Metadata<>(metadata);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getResolvedMapLocation() {
        return resolvedMapLocation;
    }

    public void setResolvedMapLocation(String resolvedMapLocation) {
        this.resolvedMapLocation = resolvedMapLocation;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "run=" + metadata +
                '}';
    }
}
