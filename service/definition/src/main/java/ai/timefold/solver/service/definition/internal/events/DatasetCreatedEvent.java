package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.dto.dataset.Metadata;

/**
 * Event that a new dataset is submitted via the API.
 */
public final class DatasetCreatedEvent extends AbstractDatasetEvent {

    public DatasetCreatedEvent(Metadata metadata) {
        super(metadata);
    }
}
