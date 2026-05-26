package ai.timefold.solver.model.jackson.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A factory for creating an {@link ObjectMapper} instance with the custom configuration
 * of the {@link SdkObjectMapperCustomizer}.
 * Important: This factory is intended for build-time usage only, since it creates a new {@link ObjectMapper} instance
 * that is not managed by the Quarkus framework.
 * <p>
 * Such instances are not guaranteed to have the same configuration, including registered modules, as the managed
 * {@link ObjectMapper}.
 * Thus, using an {@link ObjectMapper} instance created by this factory at runtime may fail in natively compiled applications.
 *
 * Should be used only in models to ensure consistent serialization/deserialization behavior.
 *
 * See {@link SdkObjectMapperCustomizer} for details on the configuration applied.
 */

public final class SdkBuildTimeObjectMapperFactory {

    private SdkBuildTimeObjectMapperFactory() {
        // Private constructor to prevent instantiation
    }

    public static ObjectMapper create() {
        /*
         * Creates a new ObjectMapper instance that is not managed by Quarkus, thus not safe to use at runtime
         * in the native mode.
         */
        ObjectMapper mapper = new ObjectMapper();
        SdkObjectMapperCustomizer customizer = new SdkObjectMapperCustomizer();
        customizer.customize(mapper);
        return mapper.findAndRegisterModules();
    }
}
