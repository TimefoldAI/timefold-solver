package ai.timefold.solver.service.definition.impl.validation;

import java.util.List;

import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.api.validation.metadata.IssueMessage;

/**
 * Internal validation issue to translate a JSON parsing error
 * to the {@link ValidationResult}.
 */
public final class JsonMappingError extends AbstractLegacyIssue {

    public JsonMappingError() {
        this(null);
    }

    public JsonMappingError(String legacyMessage) {
        super(IssueCode.of("JSON_MAPPING_ERROR"), IssueSeverity.ERROR,
                List.of(new IssueMessage("Unable to deserialize the input JSON")), legacyMessage);
    }
}
