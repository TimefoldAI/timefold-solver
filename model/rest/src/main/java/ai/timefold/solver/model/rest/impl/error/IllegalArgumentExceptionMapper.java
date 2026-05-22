package ai.timefold.solver.model.rest.impl.error;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException ex) {
        ErrorInfo errorInfo = new ErrorInfo(null, ErrorCodes.INVALID_DATA, ex.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

}
