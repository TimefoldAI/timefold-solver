package ai.timefold.solver.service.maps.service.client.impl.error;

import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

public class GoneRuntimeException extends TimefoldRuntimeException {
    public GoneRuntimeException(String code, String message, boolean recoverable) {
        super(code, message, recoverable);
    }
}
