package ai.timefold.solver.service.definition.impl.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Produces StorageObjectMapper instances with storage-specific (de)serialization settings.
 */
@ApplicationScoped
public class StorageObjectMapperProducer {

    /*
     * Inject the base ObjectMapper customized for the SDK.
     *
     * This instance is managed by the Quarkus framework, thus it includes all registered modules and customizations
     * even at native runtime.
     */
    @Inject
    ObjectMapper quarkusObjectMapper;

    @Produces
    public StorageObjectMapperWrapper create() {
        ObjectMapper objectMapper = quarkusObjectMapper.copy(); // Create a copy to avoid mutating the injected instance.
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
