package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.definition.api.configuration.ConfigurationProfile.DEFAULT_CONFIGURATION_PROFILE_ID;
import static ai.timefold.solver.service.definition.api.configuration.ConfigurationProfile.DEFAULT_CONFIGURATION_PROFILE_NAME;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import ai.timefold.solver.service.definition.api.configuration.ConfigurationProfile;
import ai.timefold.solver.service.definition.api.configuration.MapsConfiguration;
import ai.timefold.solver.service.definition.api.configuration.ResourcesConfiguration;
import ai.timefold.solver.service.definition.api.domain.RunConfiguration;
import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;
import ai.timefold.solver.service.jackson.impl.SdkBuildTimeObjectMapperFactory;
import ai.timefold.solver.service.quarkus.deployment.builditem.AdditionalDescriptorFilesBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelComponentsBuildItem;
import ai.timefold.solver.service.quarkus.deployment.builditem.ModelInfoBuildItem;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class DefaultConfigProfileProcessor {

    private static final Logger LOG = Logger.getLogger(DefaultConfigProfileProcessor.class);

    private static final String DEFAULT_CONFIG_FILE_NAME = "default-config.json";

    private static final String MODEL_CONFIG_NAME = "ai.timefold.model.default-config.name";
    private static final String MODEL_CONFIG_DESCRIPTION = "ai.timefold.model.default-config.description";
    private static final String MODEL_CONFIG_MAX_THREAD_COUNT = "ai.timefold.model.default-config.max-thread-count";
    private static final String MODEL_CONFIG_MAP_LOCATION = "ai.timefold.model.default-config.map.location";
    private static final String MODEL_CONFIG_MAP_PROVIDER = "ai.timefold.model.default-config.map.provider";
    private static final String MODEL_CONFIG_MAP_DISTANCE = "ai.timefold.model.default-config.map.max-distance-from-road";
    private static final String MODEL_CONFIG_MAP_TRANSPORT_TYPE = "ai.timefold.model.default-config.map.transport-type";

    public static final String MODEL_CONFIG_TERMINATION_SPENT_LIMIT =
            "ai.timefold.model.default-config.termination.spent-limit";
    public static final String MODEL_CONFIG_TERMINATION_UNIMPROVED_SPENT_LIMIT =
            "ai.timefold.model.default-config.termination.unimproved-spent-limit";

    private static final ObjectMapper MAPPER = SdkBuildTimeObjectMapperFactory.create()
            .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
            // Override property-level annotation to enforce including all properties.
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public JsonInclude.Value findPropertyInclusion(Annotated a) {
                    return JsonInclude.Value.construct(
                            JsonInclude.Include.ALWAYS,
                            JsonInclude.Include.ALWAYS);
                }
            });

    @BuildStep(onlyIfNot = IsDevelopment.class)
    public void generateDefaultConfigProfile(ModelInfoBuildItem modelInfo,
            ModelComponentsBuildItem modelComponentsBuildItem,
            OutputTargetBuildItem out,
            BuildProducer<AdditionalDescriptorFilesBuildItem> producer) throws Exception {

        Path directory = Paths.get(out.getOutputDirectory().toString(), "timefold", modelInfo.getModelId(),
                "default-config-profile");

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        ClassInfo modelConfigOverrides = modelComponentsBuildItem.getModelConfigOverrides();
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(modelConfigOverrides.name().toString());
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Map<String, Object> modelOverrides = MAPPER.readValue(MAPPER.writeValueAsString(instance), Map.class);

        Config config = ConfigProvider.getConfig();

        String configName = config.getOptionalValue(MODEL_CONFIG_NAME, String.class).orElse(DEFAULT_CONFIGURATION_PROFILE_NAME); // Name cannot be null
        String description = config.getOptionalValue(MODEL_CONFIG_DESCRIPTION, String.class).orElse(null);
        String provider = config.getOptionalValue(MODEL_CONFIG_MAP_PROVIDER, String.class).orElse(null);
        String location = config.getOptionalValue(MODEL_CONFIG_MAP_LOCATION, String.class).orElse(null);
        Double maxDistanceFromRoad = config.getOptionalValue(MODEL_CONFIG_MAP_DISTANCE, Double.class).orElse(null);
        String transportType = config.getOptionalValue(MODEL_CONFIG_MAP_TRANSPORT_TYPE, String.class).orElse(null);
        Integer maxThreadCount = config.getOptionalValue(MODEL_CONFIG_MAX_THREAD_COUNT, Integer.class).orElse(null);
        Duration spentLimit = config.getOptionalValue(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, Duration.class).orElse(null);
        Duration unimprovedSpentLimit =
                config.getOptionalValue(MODEL_CONFIG_TERMINATION_UNIMPROVED_SPENT_LIMIT, Duration.class).orElse(null);

        if (spentLimit == null) {
            throw new IllegalStateException(
                    String.format("Spent limit configuration for model not set (%s)", MODEL_CONFIG_TERMINATION_SPENT_LIMIT));
        }

        // unimprovedSpentLimit can be null, Diminished Returns termination is used in that case

        ConfigurationProfile configProfile =
                new ConfigurationProfile(DEFAULT_CONFIGURATION_PROFILE_ID, configName, description,
                        new MapsConfiguration(provider, location, maxDistanceFromRoad, transportType),
                        new ResourcesConfiguration(null, null),
                        new RunConfiguration(maxThreadCount,
                                new SolverTerminationConfig(spentLimit, unimprovedSpentLimit)),
                        modelOverrides, null, null);

        byte[] bytes = MAPPER.writeValueAsBytes(configProfile);
        final Path defaultConfigProfilePath = Paths.get(directory.toString(), DEFAULT_CONFIG_FILE_NAME);
        Files.write(defaultConfigProfilePath, bytes);

        producer.produce(new AdditionalDescriptorFilesBuildItem(defaultConfigProfilePath));
        LOG.debug("Generated default configuration profile with config " + configProfile);
    }
}
