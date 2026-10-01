package ai.timefold.solver.service.maps.service.client.impl.error;

import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

public class MapServiceIllegalArgumentException extends TimefoldRuntimeException {
    public MapServiceIllegalArgumentException(String code, String message, boolean recoverable) {
        super(code, message, recoverable);
    }
}
