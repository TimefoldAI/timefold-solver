package ai.timefold.solver.model.rest.impl.error;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ai.timefold.solver.model.definition.api.error.ErrorInfo;
import ai.timefold.solver.model.definition.internal.error.ItemNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ItemNotFoundExceptionMapper implements ExceptionMapper<ItemNotFoundException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(ItemNotFoundException ex) {
        LOGGER.warn("Item not found, code ({}), message ({})", ex.getCode(), ex.getMessage());

        ErrorInfo errorInfo = new ErrorInfo(null, ex.getCode(), ex.getMessage());

        return Response.status(Response.Status.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(errorInfo).build();
    }

}
