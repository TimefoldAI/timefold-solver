package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.validation.ModelValidator;

/**
 * Sent when a dataset is successfully validated by {@link ModelValidator}.
 */
public final class DatasetValidatedEvent extends AbstractDatasetEvent {

    public DatasetValidatedEvent(Metadata metadata) {
        super(metadata);
    }
}
