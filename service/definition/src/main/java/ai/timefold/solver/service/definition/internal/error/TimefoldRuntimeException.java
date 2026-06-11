package ai.timefold.solver.service.definition.internal.error;

import java.util.UUID;

public class TimefoldRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorId;

    private final String code;

    private final boolean recoverable;

    public TimefoldRuntimeException(String code, String message) {
        super(message);
        this.errorId = UUID.randomUUID().toString();
        this.code = code;
        this.recoverable = true;
    }

    public TimefoldRuntimeException(String code, String message, Throwable cause) {
        super(message, cause);
        this.errorId = UUID.randomUUID().toString();
        this.code = code;
        this.recoverable = true;
    }

    public TimefoldRuntimeException(String code, String message, boolean recoverable) {
        super(message);
        this.errorId = UUID.randomUUID().toString();
        this.code = code;
        this.recoverable = recoverable;
    }

    public TimefoldRuntimeException(String code, String message, Throwable cause, boolean recoverable) {
        super(message, cause);
        this.errorId = UUID.randomUUID().toString();
        this.code = code;
        this.recoverable = recoverable;
    }

    public String getErrorId() {
        return errorId;
    }

    public String getCode() {
        return code;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    @Override
    public String toString() {
        return "TimefoldRuntimeException [errorId=" + errorId + ", code=" + code + ", message=" + getMessage() + "]";
    }
}
