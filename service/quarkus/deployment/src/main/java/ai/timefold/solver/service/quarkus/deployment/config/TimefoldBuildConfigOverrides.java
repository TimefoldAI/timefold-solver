package ai.timefold.solver.service.quarkus.deployment.config;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimefoldBuildConfigOverrides implements ConfigSource {

    /**
     * System property through which {@code timefold:configure} hands over the absolute location of the file it
     * generated.
     * The goal writes the file into the build directory of the module it runs for,
     * which this augmentation has no way of deriving on its own:
     * the configuration is built before any build step runs,
     * so {@code OutputTargetBuildItem} is not available yet,
     * and the working directory is the directory Maven was launched in rather than the module's own directory.
     * <p>
     * Must stay in sync with {@code ai.timefold.solver.tools.maven.ConfigureMojo#BUILD_PROPERTIES_LOCATION};
     * the two modules do not share code, so the literal is deliberately duplicated and asserted on both sides.
     */
    public static final String BUILD_PROPERTIES_LOCATION = "timefold.build.properties.location";

    private static final Logger LOGGER = LoggerFactory.getLogger(TimefoldBuildConfigOverrides.class);

    private final Properties overrides = new Properties();

    public TimefoldBuildConfigOverrides() {
        this(resolveBuildPropertiesPath());
    }

    /**
     * @param timefoldBuildPropertiesPath location of the generated properties file, which need not exist
     */
    TimefoldBuildConfigOverrides(Path timefoldBuildPropertiesPath) {
        File timefoldBuildPropertiesFile = timefoldBuildPropertiesPath.toFile();
        if (timefoldBuildPropertiesFile.exists()) {
            try (FileInputStream input = new FileInputStream(timefoldBuildPropertiesFile)) {
                overrides.load(input);
            } catch (Exception e) {
                LOGGER.warn("Unable to read timefold build properties from {} due to {}",
                        timefoldBuildPropertiesFile.getAbsolutePath(), e.getMessage());
            }
        }
    }

    /**
     * Prefers the location handed over by {@code timefold:configure},
     * and falls back to the location relative to the working directory that older versions of the plugin wrote to,
     * which is also the location a hand-written file is expected at.
     */
    private static Path resolveBuildPropertiesPath() {
        String configuredLocation = System.getProperty(BUILD_PROPERTIES_LOCATION);
        if (configuredLocation != null && !configuredLocation.isBlank()) {
            return Paths.get(configuredLocation.trim());
        }
        return Paths.get("target", "generated-resources", "timefold-build.properties");
    }

    @Override
    public int getOrdinal() {
        return 500;
    }

    @Override
    public Set<String> getPropertyNames() {
        return overrides.stringPropertyNames();
    }

    @Override
    public String getValue(String propertyName) {
        return overrides.getProperty(propertyName);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

}
