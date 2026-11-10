package ai.timefold.solver.service.definition.api.dto.validation;

import java.util.Collection;

import ai.timefold.solver.service.definition.api.dto.validation.issue.Issue;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ValidationResult<ValidationIssue_ extends Issue>(
        @Schema(description = "Determines if the validated dataset is accepted for further processing",
                required = true) ValidationStatus status,
        @Schema(description = "Validation issues found") Collection<ValidationIssue_> issues) {

    @JsonIgnore
    public boolean isValid() {
        return status != ValidationStatus.ERRORS;
    }
}
