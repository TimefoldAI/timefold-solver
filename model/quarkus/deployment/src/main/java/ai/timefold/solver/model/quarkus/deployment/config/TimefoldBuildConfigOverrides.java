package ai.timefold.solver.model.quarkus.deployment.config;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimefoldBuildConfigOverrides implements ConfigSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimefoldBuildConfigOverrides.class);
    private Properties overrides = new Properties();

    public TimefoldBuildConfigOverrides() {
        File timefoldBuildPropertiesFile = Paths.get("target", "generated-resources", "timefold-build.properties").toFile();

        if (timefoldBuildPropertiesFile.exists()) {
            try (FileInputStream input = new FileInputStream(timefoldBuildPropertiesFile)) {
                overrides.load(input);
            } catch (Exception e) {
                LOGGER.warn("Unable to read timefold build properties from {} due to {}",
                        timefoldBuildPropertiesFile.getAbsolutePath(), e.getMessage());
            }
        }
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
