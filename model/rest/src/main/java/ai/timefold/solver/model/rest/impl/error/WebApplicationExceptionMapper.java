package ai.timefold.solver.model.rest.impl.error;

import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.error.ErrorInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);

    @Override
    public Response toResponse(WebApplicationException ex) {
        String id = UUID.randomUUID().toString();
        String code = String.valueOf(ex.getResponse().getStatus());
        String message = ex.getMessage();
        String details = combineMessage(ex);

        LOGGER.error("Web layer error with id ({}), code ({}), message ({})", id, code, details);

        ErrorInfo errorInfo = new ErrorInfo(id, code, message, details);

        return Response.status(ex.getResponse().getStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

    protected String combineMessage(Throwable ex) {
        StringBuilder builder = new StringBuilder(ex.getMessage());

        while (ex.getCause() != null) {
            builder.append("\n").append(ex.getCause().getMessage());
            ex = ex.getCause();
        }

        return builder.toString();
    }

}
