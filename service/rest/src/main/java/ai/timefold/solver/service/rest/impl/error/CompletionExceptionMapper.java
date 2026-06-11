package ai.timefold.solver.service.rest.impl.error;

import java.util.concurrent.CompletionException;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.service.definition.api.error.ErrorInfo;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

@Provider
public class CompletionExceptionMapper implements ExceptionMapper<CompletionException> {
    @Override
    public Response toResponse(CompletionException ex) {
        Status status = Status.INTERNAL_SERVER_ERROR;
        ErrorInfo errorInfo = new ErrorInfo(null, ErrorCodes.UNKNOWN, ex.getMessage());

        Throwable cause = ex.getCause();
        if (cause instanceof TimefoldRuntimeException) {
            errorInfo = new ErrorInfo(((TimefoldRuntimeException) cause).getErrorId(),
                    ((TimefoldRuntimeException) cause).getCode(), cause.getMessage());
        } else if (cause instanceof ItemNotFoundException) {
            status = Status.NOT_FOUND;
            errorInfo = new ErrorInfo(null,
                    ((ItemNotFoundException) cause).getCode(), cause.getMessage());
        }

        return Response.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

}
