package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.domain.Metadata;

import org.jspecify.annotations.Nullable;

/**
 * Event referencing the associated {@link Metadata}.
 */
public abstract sealed class AbstractDatasetEvent extends AbstractEvent
        permits DatasetCreatedEvent, DatasetValidatedEvent, SolverWorkerEvent {

    private final Metadata metadata;

    protected AbstractDatasetEvent(Metadata metadata) {
        super(metadata.getId());
        // Safe copy of the run to avoid external modifications.
        this.metadata = new Metadata<>(metadata);
    }

    AbstractDatasetEvent(String id) {
        super(id);
        this.metadata = null;
    }

    /**
     * The run metadata.
     *
     * @return the metadata; null only for an event made by {@link ItemFailed#withoutMetadata}
     */
    public @Nullable Metadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "run=" + metadata +
                '}';
    }
}
