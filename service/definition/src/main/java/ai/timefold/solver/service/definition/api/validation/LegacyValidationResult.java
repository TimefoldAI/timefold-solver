package ai.timefold.solver.service.definition.api.validation;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "The result of the validation of the model input", additionalProperties = Schema.False.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public record LegacyValidationResult(
        @Schema(description = "The summary of the validation. The model input passes the validation if the summary does not contain errors.") @JsonProperty ValidationStatus summary,
        @Schema(description = "The list of errors that occurred during the validation. If the list is empty, the model input is considered valid.") @JsonProperty @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> errors,
        @Schema(description = "The list of warnings that occurred during the validation.") @JsonProperty @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> warnings) {

    public static LegacyValidationResult notSupported() {
        return new LegacyValidationResult(ValidationStatus.VALIDATION_NOT_SUPPORTED, null, null);
    }

    public static LegacyValidationResult successful() {
        return new LegacyValidationResult(ValidationStatus.OK, null, null);
    }

    @JsonIgnore
    public boolean isValid() {
        return summary != ValidationStatus.ERRORS;
    }
}
