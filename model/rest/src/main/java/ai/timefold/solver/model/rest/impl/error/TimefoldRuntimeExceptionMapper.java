package ai.timefold.solver.model.rest.impl.error;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Provider
@RegisterForReflection
public class TimefoldRuntimeExceptionMapper implements ExceptionMapper<TimefoldRuntimeException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimefoldRuntimeExceptionMapper.class);

    @Override
    public Response toResponse(TimefoldRuntimeException ex) {

        LOGGER.error("Unexpected runtime exception while processing request, code ({}), id ({}), message ({})", ex.getCode(),
                ex.getErrorId(), ex.getMessage(), ex);

        ErrorInfo errorInfo = new ErrorInfo(ex.getErrorId(), ex.getCode(), ex.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

}
