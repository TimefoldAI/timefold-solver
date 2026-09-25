package ai.timefold.solver.service.definition.impl.storage;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Produces StorageObjectMapper instances with storage-specific (de)serialization settings.
 */
public class StorageObjectMapperProducer {

    private ObjectMapper quarkusObjectMapper;

    @Inject
    public StorageObjectMapperProducer(ObjectMapper quarkusObjectMapper) {
        this.quarkusObjectMapper = quarkusObjectMapper;
    }

    @Produces
    public StorageObjectMapperWrapper create() {
        var objectMapper = quarkusObjectMapper.copy(); // Create a copy to avoid mutating the injected instance.
        /*
         * Storage-specific customizations are more permissive (beyond the model JSON Schema):
         * 1. Ignore unknown properties during deserialization to allow for forward compatibility.
         * 2. Enable case-insensitive property matching as some cloud storages are case-insensitive.
         */
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.getDeserializationConfig().with(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        objectMapper.findAndRegisterModules();

        return new StorageObjectMapperWrapper(objectMapper);
    }
}
