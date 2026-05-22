package ai.timefold.solver.model.maps.service.client.impl.error;

import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;

public class MapServiceIllegalArgumentException extends TimefoldRuntimeException {
    public MapServiceIllegalArgumentException(String code, String message, boolean recoverable) {
        super(code, message, recoverable);
    }
}
