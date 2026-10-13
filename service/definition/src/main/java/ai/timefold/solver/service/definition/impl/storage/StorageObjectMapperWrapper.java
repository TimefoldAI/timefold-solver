package ai.timefold.solver.service.definition.impl.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wrapper around ObjectMapper to be injected into storage services to enforce correct (de)serialization settings.
 * <p>
 * Instances are normally produced by the {@link StorageObjectMapperProducer}.
 */
public final class StorageObjectMapperWrapper {

    private final ObjectMapper objectMapper;

    public StorageObjectMapperWrapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper get() {
        return objectMapper;
    }
}
