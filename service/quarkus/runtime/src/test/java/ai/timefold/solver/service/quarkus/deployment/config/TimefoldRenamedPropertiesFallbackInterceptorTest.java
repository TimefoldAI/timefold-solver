package ai.timefold.solver.service.quarkus.deployment.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class TimefoldRenamedPropertiesFallbackInterceptorTest {

    private static SmallRyeConfig buildConfig(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withInterceptors(new TimefoldRenamedPropertiesFallbackInterceptor())
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
    }

    private static List<LogRecord> captureLogs(Runnable action) {
        Logger julLogger = Logger.getLogger(TimefoldRenamedPropertiesFallbackInterceptor.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Level originalLevel = julLogger.getLevel();
        julLogger.setLevel(Level.ALL);
        julLogger.addHandler(handler);
        try {
            action.run();
        } finally {
            julLogger.removeHandler(handler);
            julLogger.setLevel(originalLevel);
        }
        return records;
    }

    @Test
    void fallsBackToExactLegacyName() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.application.name", "my-app"));
        assertThat(config.getRawValue("timefold.model.name")).isEqualTo("my-app");
    }

    @Test
    void prefersNewNameOverLegacyForExactRename() {
        SmallRyeConfig config = buildConfig(Map.of(
                "timefold.model.name", "new-name",
                "timefold.application.name", "legacy-name"));
        assertThat(config.getRawValue("timefold.model.name")).isEqualTo("new-name");
    }

    @Test
    void fallsBackAcrossPrefixRename_platform() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.foo.bar", "42"));
        assertThat(config.getRawValue("timefold.platform.foo.bar")).isEqualTo("42");
    }

    @Test
    void fallsBackAcrossPrefixRename_defaultConfig() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.model.default-config.something", "v"));
        assertThat(config.getRawValue("timefold.model.default-config.something")).isEqualTo("v");
    }

    @Test
    void fallsBackAcrossPrefixRename_termination() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.termination.spent-limit", "10s"));
        assertThat(config.getRawValue("timefold.model.termination.spent-limit")).isEqualTo("10s");
    }

    @Test
    void fallsBackAcrossPrefixRename_contact() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.application.contact.email", "a@b.c"));
        assertThat(config.getRawValue("timefold.model.contact.email")).isEqualTo("a@b.c");
    }

    @Test
    void fallsBackAcrossPrefixRename_rest() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.rest.path", "/api"));
        assertThat(config.getRawValue("timefold.model.rest.path")).isEqualTo("/api");
    }

    @Test
    void fallsBackAcrossPrefixRename_storage() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.storage.type", "in-memory"));
        assertThat(config.getRawValue("timefold.storage.type")).isEqualTo("in-memory");
    }

    @Test
    void fallsBackForMultipleLegacyNames_firstLegacy() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.application.id", "id-1"));
        assertThat(config.getRawValue("timefold.model.id")).isEqualTo("id-1");
    }

    @Test
    void fallsBackForMultipleLegacyNames_secondLegacy() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.model", "id-2"));
        assertThat(config.getRawValue("timefold.model.id")).isEqualTo("id-2");
    }

    @Test
    void newNameWinsOverMultipleLegacyNames() {
        SmallRyeConfig config = buildConfig(Map.of(
                "timefold.model.id", "new-id",
                "timefold.application.id", "legacy-id-a",
                "ai.timefold.platform.model", "legacy-id-b"));
        assertThat(config.getRawValue("timefold.model.id")).isEqualTo("new-id");
    }

    @Test
    void apiVersionResolvesFromLegacyVersion() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.application.version", "v1"));
        assertThat(config.getRawValue("timefold.model.api-version")).isEqualTo("v1");
    }

    @Test
    void apiVersionResolvesFromLegacyModelVersion() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.model-version", "v1"));
        assertThat(config.getRawValue("timefold.model.api-version")).isEqualTo("v1");
    }

    @Test
    void restResourceLegacyRename() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.model-resource", "/models"));
        assertThat(config.getRawValue("timefold.model.rest-resource")).isEqualTo("/models");
    }

    @Test
    void schemaValidationLegacyRename() {
        SmallRyeConfig config = buildConfig(Map.of("ai.timefold.platform.models.validation.enable", "true"));
        assertThat(config.getRawValue("timefold.model.schema.validation.enable")).isEqualTo("true");
    }

    @Test
    void unknownPropertyReturnsNull() {
        SmallRyeConfig config = buildConfig(Map.of());
        assertThat(config.getRawValue("timefold.model.name")).isNull();
    }

    @Test
    void logsWarningWhenLegacyPropertyIsDefined() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.application.name", "my-app"));

        List<LogRecord> records = captureLogs(() -> config.getRawValue("timefold.model.name"));

        assertThat(records)
                .anySatisfy(record -> assertThat(record.getMessage()).contains("Deprecated configuration key"));
    }

    @Test
    void doesNotLogWarningWhenOnlyNewPropertyIsDefined() {
        SmallRyeConfig config = buildConfig(Map.of("timefold.model.name", "my-app"));

        List<LogRecord> records = captureLogs(() -> config.getRawValue("timefold.model.name"));

        assertThat(records).isEmpty();
    }
}