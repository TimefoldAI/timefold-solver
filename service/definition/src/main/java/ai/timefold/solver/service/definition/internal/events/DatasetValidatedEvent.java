package ai.timefold.solver.service.definition.internal.events;

import ai.timefold.solver.service.definition.api.dto.dataset.Metadata;
import ai.timefold.solver.service.definition.spi.validation.ModelValidator;

/**
 * Sent when a dataset is successfully validated by {@link ModelValidator}.
 */
public final class DatasetValidatedEvent extends AbstractDatasetEvent {

    private final ValidationSummary validationSummary;

    public DatasetValidatedEvent(Metadata metadata) {
        this(metadata, null);
    }

    public DatasetValidatedEvent(Metadata metadata, ValidationSummary validationSummary) {
        super(metadata);
        this.validationSummary = validationSummary;
    }

    public ValidationSummary getValidationSummary() {
        return validationSummary;
    }
}
