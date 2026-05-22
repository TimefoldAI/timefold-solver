package ai.timefold.solver.model.maps.service.client.impl.error;

import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;

public class GoneRuntimeException extends TimefoldRuntimeException {
    public GoneRuntimeException(String code, String message, boolean recoverable) {
        super(code, message, recoverable);
    }
}
