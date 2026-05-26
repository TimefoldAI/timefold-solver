package ai.timefold.solver.model.rest.impl.error;

import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class UnknownExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnknownExceptionMapper.class);

    @Override
    public Response toResponse(Throwable ex) {
        String id = UUID.randomUUID().toString();
        String code = ErrorCodes.UNKNOWN;
        if (ex instanceof TimefoldRuntimeException) {
            id = ((TimefoldRuntimeException) ex).getErrorId();
            code = ((TimefoldRuntimeException) ex).getCode();
        }
        LOGGER.error("Unexpected error during processing request with id ({}), code ({}), message ({})", id, code,
                ex.getMessage(), ex);

        ErrorInfo errorInfo = new ErrorInfo(id, code,
                "Unexpected processing error occured, please contact Timefold support with code and id of the error");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

}
