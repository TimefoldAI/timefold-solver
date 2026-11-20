package ai.timefold.solver.service.quarkus.deployment.rest.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import ai.timefold.solver.service.definition.api.dto.dataset.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.dto.patch.ModelInputPatchRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractModelInputPatchRequestMessageBodyReaders<Config_ extends ModelConfigOverrides>
        implements MessageBodyReader<ModelInputPatchRequest<Config_>> {

    @Inject
    ObjectMapper mapper;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ModelInputPatchRequest.class;
    }

    @Override
    public ModelInputPatchRequest<Config_> readFrom(Class<ModelInputPatchRequest<Config_>> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return mapper.readValue(entityStream, typeRef());
    }

    protected abstract TypeReference<ModelInputPatchRequest<Config_>> typeRef();
}
