package ai.timefold.solver.service.quarkus.deployment.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.util.List;
import java.util.Map;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * Falls back to the property names used before the "unify configuration properties" rename, so that models built
 * against the old names keep working without changes.
 */
public class TimefoldRenamedPropertiesFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final Map<String, String> RENAMED_PROPERTIES = Map.ofEntries(
            // Model metadata / application info.
            Map.entry("timefold.model.name", "timefold.application.name"),
            Map.entry("timefold.model.description", "timefold.application.description"),
            Map.entry("timefold.model.build-timestamp", "timefold.application.build-timestamp"),
            Map.entry("timefold.model.contact.email", "timefold.application.contact.email"),
            Map.entry("timefold.model.contact.name", "timefold.application.contact.name"),
            Map.entry("timefold.model.contact.url", "timefold.application.contact.url"),
            Map.entry("timefold.model.max-thread-count", "ai.timefold.model.max-thread-count"),
            Map.entry("timefold.model.rest-resource", "ai.timefold.platform.model-resource"),
            Map.entry("timefold.model.schema.validation.enable", "ai.timefold.platform.models.validation.enable"),
            Map.entry("timefold.model.test.serialization.callback.disable",
                    "ai.timefold.platform.model.test.serialization.callback.disable"),

            // Default configuration profile.
            Map.entry("timefold.model.default-config.name", "ai.timefold.model.default-config.name"),
            Map.entry("timefold.model.default-config.description", "ai.timefold.model.default-config.description"),
            Map.entry("timefold.model.default-config.max-thread-count",
                    "ai.timefold.model.default-config.max-thread-count"),
            Map.entry("timefold.model.default-config.map.provider", "ai.timefold.model.default-config.map.provider"),
            Map.entry("timefold.model.default-config.map.location", "ai.timefold.model.default-config.map.location"),
            Map.entry("timefold.model.default-config.map.max-distance-from-road",
                    "ai.timefold.model.default-config.map.max-distance-from-road"),
            Map.entry("timefold.model.default-config.map.transport-type",
                    "ai.timefold.model.default-config.map.transport-type"),
            Map.entry("timefold.model.default-config.map.use-traffic",
                    "ai.timefold.model.default-config.map.use-traffic"),
            Map.entry("timefold.model.default-config.termination.spent-limit",
                    "ai.timefold.model.default-config.termination.spent-limit"),
            Map.entry("timefold.model.default-config.termination.unimproved-spent-limit",
                    "ai.timefold.model.default-config.termination.unimproved-spent-limit"),

            // Solver termination.
            Map.entry("timefold.model.termination.spent-limit", "ai.timefold.platform.termination.spent-limit"),
            Map.entry("timefold.model.termination.maximum-spent-limit",
                    "ai.timefold.platform.termination.maximum-spent-limit"),
            Map.entry("timefold.model.termination.unimproved-spent-limit",
                    "ai.timefold.platform.termination.unimproved-spent-limit"),
            Map.entry("timefold.model.termination.maximum-unimproved-spent-limit",
                    "ai.timefold.platform.termination.maximum-unimproved-spent-limit"),
            Map.entry("timefold.model.termination.step-count-limit",
                    "ai.timefold.platform.termination.step-count-limit"),
            Map.entry("timefold.model.termination.best-score-limit",
                    "ai.timefold.platform.termination.best-score-limit"),

            // Storage.
            Map.entry("timefold.storage.type", "ai.timefold.storage.type"),
            Map.entry("timefold.storage.bucket", "ai.timefold.storage.bucket"),
            Map.entry("timefold.storage.path", "ai.timefold.storage.path"),
            Map.entry("timefold.storage.ttl", "ai.timefold.storage.ttl"),

            // Map service.
            Map.entry("timefold.platform.map-service.use-traffic", "ai.timefold.platform.map-service.use-traffic"),
            Map.entry("timefold.platform.map-service.enable-fallback",
                    "ai.timefold.platform.map-service.enable-fallback"),
            Map.entry("timefold.platform.map-service.default-timeframe",
                    "ai.timefold.platform.map-service.default-timeframe"),
            Map.entry("timefold.platform.map-service.provider", "ai.timefold.platform.map-service.provider"),
            Map.entry("timefold.platform.map-service.location", "ai.timefold.platform.map-service.location"),
            Map.entry("timefold.platform.map-service.max-distance-from-road",
                    "ai.timefold.platform.map-service.max-distance-from-road"),
            Map.entry("timefold.platform.map-service.transport-type",
                    "ai.timefold.platform.map-service.transport-type"),
            Map.entry("timefold.platform.map-service.use-remote", "ai.timefold.platform.map-service.use-remote"),
            Map.entry("timefold.platform.map-service.url", "ai.timefold.platform.map-service.url"),
            Map.entry("timefold.platform.tenant-id", "ai.timefold.platform.tenant-id"));

    // Properties that each replace two distinct legacy properties, so we check both legacy names.
    private static final Map<String, List<String>> RENAMED_PROPERTIES_WITH_MULTIPLE_LEGACY_NAMES = Map.of(
            "timefold.model.id", List.of("timefold.application.id", "ai.timefold.platform.model"),
            "timefold.model.api-version", List.of("timefold.application.version", "ai.timefold.platform.model-version"));

    public TimefoldRenamedPropertiesFallbackInterceptor() {
        super(RENAMED_PROPERTIES);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        List<String> legacyNames = RENAMED_PROPERTIES_WITH_MULTIPLE_LEGACY_NAMES.get(name);
        if (legacyNames == null) {
            return super.getValue(context, name);
        }

        ConfigValue value = context.proceed(name);
        for (String legacyName : legacyNames) {
            ConfigValue legacyValue = context.proceed(legacyName);
            if (legacyValue != null && (value == null || CONFIG_SOURCE_COMPARATOR.compare(legacyValue, value) > 0)) {
                value = legacyValue.withName(name);
            }
        }
        return value;
    }
}
