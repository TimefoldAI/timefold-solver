package ai.timefold.solver.service.definition.api.validation;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public enum IssueSeverity {
    /**
     * If an issue of the ERROR severity is found during validation, the dataset cannot be further processed.
     */
    @Schema(description = "If an issue of the ERROR severity is found during validation, the dataset cannot be further processed.")
    ERROR,

    /**
     * Warnings are issues in the dataset that do not prevent from its further processing, but may impact the quality
     * of the attained solutions.
     */
    @Schema(description = "Warnings are issues in the dataset that do not prevent from its further processing, but may impact the quality of the attained solutions.")
    WARNING,
}
