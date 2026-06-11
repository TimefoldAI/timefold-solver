package ai.timefold.solver.service.test.impl.quarkus;

import ai.timefold.solver.service.jackson.impl.SdkBuildTimeObjectMapperFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * The default RestAssured object mapper serializes LocalDate as an array and by that fails platform schema validation.
 * <p>
 * This Quarkus test before class callback configures {@link RestAssuredConfig#objectMapperConfig(ObjectMapperConfig)}
 * to use the {@link ObjectMapper} customized with the SDK defaults:
 * </p>
 * <ul>
 * <li>Enables failing on unknown properties.</li>
 * <li>Serialize only non-null properties by default {@link JsonInclude.Include#NON_NULL}.</li>
 * <li>Serialize datetime and duration objects as strings, rather than numeric timestamps.</li>
 * <li>Prevents adjusting the time zone offset values to the context time zone when serializing/deserializing.</li>
 * </ul>
 * <p>
 * A concrete model can opt-out of this behaviour by setting the system property
 * {@value DISABLE_SERIALIZATION_CALLBACK_PROPERTY_NAME} to <code>true</code>.
 * </p>
 * Note: A plain jUnit extension does not work due to classloading issues because Quarkus has to run tests in a custom
 * classloader which JUnit is not aware of.
 */
public class ModelRequestSerializationCallback implements QuarkusTestBeforeClassCallback {

    public static final Logger LOGGER = LoggerFactory.getLogger(ModelRequestSerializationCallback.class);

    public static final String DISABLE_SERIALIZATION_CALLBACK_PROPERTY_NAME =
            "ai.timefold.platform.model.test.serialization.callback.disable";

    private static final ObjectMapper mapper = SdkBuildTimeObjectMapperFactory.create();

    private RestAssuredConfig restAssuredConfig;

    @Override
    public void beforeClass(Class<?> testClass) {
        if (!Boolean.getBoolean(DISABLE_SERIALIZATION_CALLBACK_PROPERTY_NAME)) {
            LOGGER.debug("Setting RestAssured objectMapper configuration before test class.");
            RestAssured.config = getRestAssuredConfig();
        } else {
            LOGGER.debug("Model request serialization callback disabled by system property '{}'.",
                    DISABLE_SERIALIZATION_CALLBACK_PROPERTY_NAME);
        }
    }

    private RestAssuredConfig getRestAssuredConfig() {
        if (restAssuredConfig == null) {
            restAssuredConfig = createConfig();
        }
        return restAssuredConfig;
    }

    private RestAssuredConfig createConfig() {
        return RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (aClass, s) -> mapper));
    }
}
