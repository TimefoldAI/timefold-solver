package ai.timefold.solver.service.maps.service.client.impl.error;

import jakarta.ws.rs.core.Response;

import ai.timefold.solver.service.definition.api.error.ErrorInfo;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import io.smallrye.common.annotation.Blocking;

@Blocking
public class MapServiceExceptionMapper implements ResponseExceptionMapper<Exception> {

    @Override
    public Exception toThrowable(Response response) {
        String message = null;
        String code = null;

        if (response.hasEntity()) {
            try {
                ErrorInfo errorMessage = response.readEntity(ErrorInfo.class);
                message = errorMessage.getMessage();
                code = errorMessage.getCode();
            } catch (Exception e) {
                message = response.readEntity(String.class);
            }
        }

        if (message == null || message.isEmpty()) {
            message = "Client error on Maps Service, status code: " + response.getStatus();
        }

        if (response.getStatus() == Response.Status.GONE.getStatusCode()) {
            code = ErrorCodes.MAP_SERVICE_CLIENT_REQUEST_ERROR;
            return new GoneRuntimeException(code, message, false);
        }

        if (response.getStatus() >= 400 && response.getStatus() < 500) {
            if (code == null || code.isEmpty()) {
                code = ErrorCodes.MAP_SERVICE_CLIENT_REQUEST_ERROR;
            }
            return new MapServiceIllegalArgumentException(code, message, false);
        } else {
            if (code == null || code.isEmpty()) {
                code = ErrorCodes.MAP_SERVICE_CLIENT_REQUEST_ERROR;
            }
            return new TimefoldRuntimeException(code, message, false);
        }
    }
}
