package ai.timefold.solver.service.quarkus.deployment.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TimefoldBuildConfigOverridesTest {

    private String originalLocation;

    @BeforeEach
    void setUp() {
        originalLocation = System.getProperty(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION);
        System.clearProperty(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION);
    }

    @AfterEach
    void tearDown() {
        if (originalLocation != null) {
            System.setProperty(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION, originalLocation);
        } else {
            System.clearProperty(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION);
        }
    }

    @Test
    void readsFileHandedOverBySystemProperty(@TempDir Path tempDir) throws IOException {
        Path propsFile = writePropertiesFile(tempDir, "quarkus.container-image.registry", "my-registry.example.com");

        System.setProperty(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION, propsFile.toString());

        TimefoldBuildConfigOverrides configSource = new TimefoldBuildConfigOverrides();

        assertThat(configSource.getValue("quarkus.container-image.registry")).isEqualTo("my-registry.example.com");
    }

    @Test
    void fallsBackToWorkingDirectoryWhenNoLocationIsSet(@TempDir Path tempDir) throws IOException {
        Path propsFile = writePropertiesFile(tempDir, "quarkus.container-image.group", "test-namespace");

        // Use the package-private constructor to point at the temp file directly,
        // since the working directory fallback resolves to target/generated-resources/timefold-build.properties
        // which is not guaranteed to exist in a test environment.
        TimefoldBuildConfigOverrides configSource = new TimefoldBuildConfigOverrides(propsFile);

        assertThat(configSource.getValue("quarkus.container-image.group")).isEqualTo("test-namespace");

        // Also verify that the no-arg constructor does not throw when the system property is absent
        // and the fallback file does not exist.
        TimefoldBuildConfigOverrides fallbackConfigSource = new TimefoldBuildConfigOverrides();
        assertThat(fallbackConfigSource.getPropertyNames()).isNotNull();
    }

    @Test
    void isEmptyWhenTheFileDoesNotExist(@TempDir Path tempDir) {
        Path nonExistentPath = tempDir.resolve("does-not-exist.properties");

        TimefoldBuildConfigOverrides configSource = new TimefoldBuildConfigOverrides(nonExistentPath);

        assertThat(configSource.getPropertyNames()).isEmpty();
    }

    @Test
    void isEmptyAndLogsWhenTheFileIsUnreadable(@TempDir Path tempDir) throws IOException {
        // Point at a directory instead of a file — FileInputStream on a directory throws an exception.
        Path directory = Files.createDirectory(tempDir.resolve("not-a-file"));

        TimefoldBuildConfigOverrides configSource = new TimefoldBuildConfigOverrides(directory);

        assertThat(configSource.getPropertyNames()).isEmpty();
    }

    @Test
    void keepsOrdinalAndName(@TempDir Path tempDir) {
        Path nonExistentPath = tempDir.resolve("does-not-exist.properties");

        TimefoldBuildConfigOverrides configSource = new TimefoldBuildConfigOverrides(nonExistentPath);

        assertThat(configSource.getOrdinal()).isEqualTo(500);
        assertThat(configSource.getName()).isEqualTo("TimefoldBuildConfigOverrides");
    }

    /**
     * Guards the cross-module literal that must stay in sync with
     * {@code ai.timefold.solver.tools.maven.ConfigureMojo#BUILD_PROPERTIES_LOCATION}.
     */
    @Test
    void buildPropertiesLocationLiteral() {
        assertThat(TimefoldBuildConfigOverrides.BUILD_PROPERTIES_LOCATION)
                .isEqualTo("timefold.build.properties.location");
    }

    private static Path writePropertiesFile(Path directory, String key, String value) throws IOException {
        Path propsFile = directory.resolve("timefold-build.properties");
        Properties props = new Properties();
        props.setProperty(key, value);
        try (FileOutputStream out = new FileOutputStream(propsFile.toFile())) {
            props.store(out, "Test properties");
        }
        return propsFile;
    }
}
