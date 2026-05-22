package ai.timefold.solver.model.definition.api.error;

import java.util.List;
import java.util.UUID;

import ai.timefold.solver.model.definition.internal.error.ErrorCodes;

public class ValidationErrorInfo {

    private final String id;
    private final String code;
    private String message;
    private List<String> details;

    public ValidationErrorInfo(List<String> details) {
        this(details, "Submitted data set is invalid according to the model's schema");
    }

    public ValidationErrorInfo(List<String> details, String message) {
        this.id = UUID.randomUUID().toString();
        this.code = ErrorCodes.INVALID_JSON_PAYLOAD;
        this.message = message;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }
}
