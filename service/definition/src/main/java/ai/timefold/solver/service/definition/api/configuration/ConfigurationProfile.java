package ai.timefold.solver.service.definition.api.configuration;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.service.definition.api.domain.RunConfiguration;

public class ConfigurationProfile {

    public static final String DEFAULT_CONFIGURATION_PROFILE_ID = "0";
    public static final String DEFAULT_CONFIGURATION_PROFILE_NAME = "Standard profile";

    private String id;

    private String name;

    private String description;

    private MapsConfiguration mapsConfiguration;

    private ResourcesConfiguration resourcesConfiguration;

    private RunConfiguration runConfiguration;

    private Map<String, Object> modelConfiguration;

    private OffsetDateTime updatedAt;

    private String defaultConfigProfileId;

    public ConfigurationProfile() {

    }

    public ConfigurationProfile(String id, String name, String description,
            MapsConfiguration mapsConfiguration, ResourcesConfiguration resourcesConfiguration,
            RunConfiguration runConfiguration,
            Map<String, Object> modelConfiguration, OffsetDateTime updatedAt, String defaultConfigProfileId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.mapsConfiguration = mapsConfiguration;
        this.runConfiguration = runConfiguration;
        this.resourcesConfiguration = resourcesConfiguration;
        this.modelConfiguration = modelConfiguration;
        this.updatedAt = updatedAt;
        this.defaultConfigProfileId = defaultConfigProfileId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MapsConfiguration getMapsConfiguration() {
        return mapsConfiguration;
    }

    public void setMapsConfiguration(MapsConfiguration mapsConfiguration) {
        this.mapsConfiguration = mapsConfiguration;
    }

    public ResourcesConfiguration getResourcesConfiguration() {
        return resourcesConfiguration;
    }

    public void setResourcesConfiguration(ResourcesConfiguration resourcesConfiguration) {
        this.resourcesConfiguration = resourcesConfiguration;
    }

    public RunConfiguration getRunConfiguration() {
        return runConfiguration;
    }

    public void setRunConfiguration(RunConfiguration runConfiguration) {
        this.runConfiguration = runConfiguration;
    }

    public Map<String, Object> getModelConfiguration() {
        return modelConfiguration;
    }

    public void setModelConfiguration(Map<String, Object> modelConfiguration) {
        this.modelConfiguration = modelConfiguration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDefaultConfigProfileId() {
        return defaultConfigProfileId;
    }

    public void setDefaultConfigProfileId(String defaultConfigProfile) {
        this.defaultConfigProfileId = defaultConfigProfile;
    }

    @Override
    public String toString() {
        return "(id=" + id + ", name=" + name + ", description=" + description + ", lastUpdated=" + updatedAt +
                ", mapsConfiguration=" + mapsConfiguration + ", runConfiguration=" + runConfiguration +
                ", modelConfiguration=" + modelConfiguration + ", resourcesConfiguration=" + resourcesConfiguration +
                ", updatedAt=" + updatedAt + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigurationProfile other = (ConfigurationProfile) obj;
        return Objects.equals(id, other.id);
    }

    public ConfigurationProfile copy() {
        ConfigurationProfile copy = new ConfigurationProfile();
        copy.setMapsConfiguration(mapsConfiguration);
        copy.setResourcesConfiguration(resourcesConfiguration);
        copy.setRunConfiguration(runConfiguration);
        if (modelConfiguration != null && !modelConfiguration.isEmpty()) {
            copy.setModelConfiguration(new HashMap<>(modelConfiguration));
        }
        copy.setDescription(description);
        copy.setUpdatedAt(updatedAt);
        copy.setName(name);
        copy.setDefaultConfigProfileId(defaultConfigProfileId);
        copy.setId(id);
        return copy;
    }

    public ConfigurationProfile merge(ConfigurationProfile entry) {

        if (entry == null) {
            return this;
        }

        if (entry.getName() != null) {
            this.name = entry.getName();
        }

        if (entry.getDescription() != null) {
            this.description = entry.getDescription();
        }

        if (entry.getMapsConfiguration() != null) {
            this.mapsConfiguration = entry.getMapsConfiguration();
        }

        if (entry.getResourcesConfiguration() != null) {
            this.resourcesConfiguration = entry.getResourcesConfiguration();
        }

        if (entry.getRunConfiguration() != null) {
            this.runConfiguration = entry.getRunConfiguration();
        }

        if (entry.getModelConfiguration() != null && !entry.getModelConfiguration().isEmpty()) {
            this.modelConfiguration = new HashMap<>(entry.getModelConfiguration());
        }

        if (entry.getUpdatedAt() != null) {
            this.updatedAt = entry.getUpdatedAt();
        }

        if (entry.getDefaultConfigProfileId() != null) {
            this.defaultConfigProfileId = entry.getDefaultConfigProfileId();
        }

        return this;
    }

    public ConfigurationProfile override(ConfigurationProfile configuration) {
        if (configuration == null) {
            return this;
        }

        if (this.getDescription() == null) {
            this.description = configuration.getDescription();
        }

        if (this.getMapsConfiguration() == null) {
            this.mapsConfiguration = configuration.getMapsConfiguration();
        } else {
            this.mapsConfiguration = this.mapsConfiguration.override(configuration.mapsConfiguration);
        }

        if (this.getResourcesConfiguration() == null) {
            this.resourcesConfiguration = configuration.getResourcesConfiguration();
        } else {
            this.resourcesConfiguration = this.resourcesConfiguration.override(configuration.resourcesConfiguration);
        }

        if (this.getRunConfiguration() == null) {
            this.runConfiguration = configuration.getRunConfiguration();
        } else {
            this.runConfiguration = this.runConfiguration.override(configuration.runConfiguration);
        }

        if (configuration.getModelConfiguration() != null && !configuration.getModelConfiguration().isEmpty()) {

            if (this.getModelConfiguration() == null) {
                this.modelConfiguration = new HashMap<>();
            } else {
                this.modelConfiguration = new HashMap<>(this.modelConfiguration);
            }

            // Model configurations should be merged
            configuration.getModelConfiguration().forEach((k, v) -> this.modelConfiguration.putIfAbsent(k, v));
        }

        return this;
    }
}
