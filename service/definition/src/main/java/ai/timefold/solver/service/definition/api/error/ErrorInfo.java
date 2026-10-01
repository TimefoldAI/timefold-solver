package ai.timefold.solver.service.definition.api.error;

import java.util.UUID;

import ai.timefold.solver.service.definition.internal.error.ErrorCodes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorInfo {

    public static final String LOG_ENTRY_SEPARATOR = "\n======================================================\n";

    private final String id;
    private final String code;
    private String message;
    private String details;

    public ErrorInfo() {
        this.id = UUID.randomUUID().toString();
        this.code = ErrorCodes.UNKNOWN;
    }

    public ErrorInfo(String id, String code, String message) {
        this(id, code, message, null);
    }

    public ErrorInfo(String id, String code, String message, String details) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.code = code == null ? ErrorCodes.UNKNOWN : code;
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

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "error id (" + id + "), code (" + code + "), message (" + message + ")";
    }

    public void appendPreviousLog(String message, String details) {

        this.details = message + "\n" + details + LOG_ENTRY_SEPARATOR + this.details;
    }
}
