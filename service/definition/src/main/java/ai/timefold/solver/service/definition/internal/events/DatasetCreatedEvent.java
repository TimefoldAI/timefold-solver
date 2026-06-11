package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.domain.Metadata;

/**
 * Event that a new dataset is submitted via the API.
 */
public final class DatasetCreatedEvent extends AbstractDatasetEvent {

    public DatasetCreatedEvent(Metadata metadata) {
        super(metadata);
    }
}
