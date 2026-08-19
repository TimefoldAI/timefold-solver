package ai.timefold.solver.service.quarkus.deployment.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;

/**
 * Falls back to the property names used before the "unify configuration properties" rename, so that models built
 * against the old names keep working without changes.
 */
@Priority(Priorities.LIBRARY + 290)
public class TimefoldRenamedPropertiesFallbackInterceptor extends FallbackConfigSourceInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimefoldRenamedPropertiesFallbackInterceptor.class);

    // Renames that don't follow any of the prefix patterns below.
    private static final Map<String, String> RENAMED_EXACT_PROPERTIES = Map.ofEntries(
            Map.entry("timefold.model.name", "timefold.application.name"),
            Map.entry("timefold.model.description", "timefold.application.description"),
            Map.entry("timefold.model.build-timestamp", "timefold.application.build-timestamp"),
            Map.entry("timefold.model.max-thread-count", "ai.timefold.model.max-thread-count"),
            Map.entry("timefold.model.rest-resource", "ai.timefold.platform.model-resource"),
            Map.entry("timefold.model.schema.validation.enable", "ai.timefold.platform.models.validation.enable"),
            Map.entry("timefold.model.test.serialization.callback.disable",
                    "ai.timefold.platform.model.test.serialization.callback.disable"),
            Map.entry("timefold.tenant.store", "ai.timefold.tenant.store"),
            Map.entry("timefold.maps.store", "ai.timefold.maps.store"),
            Map.entry("timefold.models.store", "ai.timefold.models.store"),
            Map.entry("timefold.plan.store", "ai.timefold.plan.store"));

    // Renames of every property under a given prefix, checked in order; the first match wins.
    private static final Map<String, String> RENAMED_PROPERTY_PREFIXES = Map.ofEntries(
            Map.entry("timefold.model.default-config.", "ai.timefold.model.default-config."),
            Map.entry("timefold.model.termination.", "ai.timefold.platform.termination."),
            Map.entry("timefold.model.contact.", "timefold.application.contact."),
            Map.entry("timefold.model.rest.", "timefold.rest."),
            Map.entry("timefold.storage.", "ai.timefold.storage."),
            Map.entry("timefold.platform.", "ai.timefold.platform."));

    // Properties that each replace two distinct legacy properties, so we check both legacy names.
    private static final Map<String, List<String>> RENAMED_PROPERTIES_WITH_MULTIPLE_LEGACY_NAMES = Map.of(
            "timefold.model.id", List.of("timefold.application.id", "ai.timefold.platform.model"),
            "timefold.model.api-version", List.of("timefold.application.version", "ai.timefold.platform.model-version"));

    public TimefoldRenamedPropertiesFallbackInterceptor() {
        super(TimefoldRenamedPropertiesFallbackInterceptor::toLegacyName);
    }

    private static String toLegacyName(String name) {
        var exactLegacyName = RENAMED_EXACT_PROPERTIES.get(name);
        if (exactLegacyName != null) {
            logWarningForResolvedKey(name, exactLegacyName);
            return exactLegacyName;
        }
        for (Map.Entry<String, String> prefix : RENAMED_PROPERTY_PREFIXES.entrySet()) {
            if (name.startsWith(prefix.getKey())) {
                var legacyName = prefix.getValue() + name.substring(prefix.getKey().length());
                logWarningForResolvedKey(name, legacyName);
                return legacyName;
            }
        }
        return name;
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
                logWarningForResolvedKey(name, legacyName);
                value = legacyValue.withName(name);
            }
        }
        return value;
    }

    private static void logWarningForResolvedKey(String key, String legacyKey) {
        LOGGER.warn("Deprecated configuration key '{}' has been resolved to '{}'.\nPlease use the '{}' instead.",
                legacyKey, key, key);
    }
}
