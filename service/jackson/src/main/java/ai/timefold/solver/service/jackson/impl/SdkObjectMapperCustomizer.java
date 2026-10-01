package ai.timefold.solver.service.jackson.impl;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Customizes the Jackson {@link ObjectMapper} with the defaults used by the SDK:
 * <ul>
 * <li>Enables failing on unknown properties.</li>
 * <li>Serialize only non-null properties by default {@link JsonInclude.Include#NON_NULL}.</li>
 * <li>Serialize datetime and duration objects as strings, rather than numeric timestamps.</li>
 * <li>Prevents adjusting the time zone offset values to the context time zone when serializing/deserializing.</li>
 * </ul>
 * <p>
 * Based on Quarkus <a href="https://quarkus.io/guides/rest-json#jackson">Writing JSON REST services</a> guide.
 */
@Singleton
public class SdkObjectMapperCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                // use this feature only in a model, the customizer should not leak into the api-gateway of the platform
                // (RunConfigurationDTO relies on ignoring unknown properties)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                // serialize datetime/duration as strings, not timestamps
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                // preserve time zones
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE);
    }
}
