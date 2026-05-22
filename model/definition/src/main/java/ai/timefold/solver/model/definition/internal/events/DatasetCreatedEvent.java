package ai.timefold.solver.model.definition.internal.events;

import ai.timefold.solver.model.definition.api.domain.Metadata;

/**
 * Event that a new dataset is submitted via the API.
 */
public final class DatasetCreatedEvent extends AbstractDatasetEvent {

    public DatasetCreatedEvent(Metadata metadata) {
        super(metadata);
    }
}
