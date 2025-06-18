package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "minimumRuinedCount",
        "maximumRuinedCount",
        "minimumRuinedPercentage",
        "maximumRuinedPercentage",
        "entitySelectorConfig",
        "variableName"
})
public class RuinRecreateMoveSelectorConfig extends MoveSelectorConfig<RuinRecreateMoveSelectorConfig> {

    // Determined by benchmarking on multiple datasets.
    private static final int DEFAULT_MINIMUM_RUINED_COUNT = 5;
    private static final int DEFAULT_MAXIMUM_RUINED_COUNT = 20;

    public static final String XML_ELEMENT_NAME = "ruinRecreateMoveSelector";

    protected Integer minimumRuinedCount = null;
    protected Integer maximumRuinedCount = null;

    protected Double minimumRuinedPercentage = null;
    protected Double maximumRuinedPercentage = null;

    @XmlElement(name = "entitySelector")
    protected EntitySelectorConfig entitySelectorConfig = null;
    protected String variableName = null;

    // **************************
    // Getters/Setters
    // **************************

    public @Nullable Integer getMinimumRuinedCount() {
        return minimumRuinedCount;
    }

    public void setMinimumRuinedCount(@Nullable Integer minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
    }

    public @NonNull RuinRecreateMoveSelectorConfig withMinimumRuinedCount(@NonNull Integer minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
        return this;
    }

    public @Nullable Integer getMaximumRuinedCount() {
        return maximumRuinedCount;
    }

    public void setMaximumRuinedCount(@Nullable Integer maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
    }

    public @NonNull RuinRecreateMoveSelectorConfig withMaximumRuinedCount(@NonNull Integer maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
        return this;
    }

    public @Nullable Double getMinimumRuinedPercentage() {
        return minimumRuinedPercentage;
    }

    public void setMinimumRuinedPercentage(@Nullable Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
    }

    public @NonNull RuinRecreateMoveSelectorConfig withMinimumRuinedPercentage(@NonNull Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
        return this;
    }

    public @Nullable Double getMaximumRuinedPercentage() {
        return maximumRuinedPercentage;
    }

    public void setMaximumRuinedPercentage(@Nullable Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
    }

    public EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public @NonNull RuinRecreateMoveSelectorConfig withMaximumRuinedPercentage(@NonNull Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
        return this;
    }

    public @NonNull RuinRecreateMoveSelectorConfig
            withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull RuinRecreateMoveSelectorConfig withVariableName(@NonNull String variableName) {
        this.setVariableName(variableName);
        return this;
    }

    // **************************
    // Interface methods
    // **************************

    @Override
    public boolean hasNearbySelectionConfig() {
        return false;
    }

    @Override
    public @NonNull RuinRecreateMoveSelectorConfig copyConfig() {
        return new RuinRecreateMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public @NonNull RuinRecreateMoveSelectorConfig inherit(@NonNull RuinRecreateMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        minimumRuinedCount =
                ConfigUtils.inheritOverwritableProperty(minimumRuinedCount, inheritedConfig.getMinimumRuinedCount());
        maximumRuinedCount =
                ConfigUtils.inheritOverwritableProperty(maximumRuinedCount, inheritedConfig.getMaximumRuinedCount());
        minimumRuinedPercentage =
                ConfigUtils.inheritOverwritableProperty(minimumRuinedPercentage, inheritedConfig.getMinimumRuinedPercentage());
        maximumRuinedPercentage =
                ConfigUtils.inheritOverwritableProperty(maximumRuinedPercentage, inheritedConfig.getMaximumRuinedPercentage());
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }

    public int determineMinimumRuinedCount(long entityCount) {
        if (minimumRuinedCount != null) {
            return minimumRuinedCount;
        }
        if (minimumRuinedPercentage != null) {
            return (int) Math.floor(minimumRuinedPercentage * entityCount);
        }
        return (int) Math.min(DEFAULT_MINIMUM_RUINED_COUNT, entityCount);
    }

    public int determineMaximumRuinedCount(long entityCount) {
        if (maximumRuinedCount != null) {
            return maximumRuinedCount;
        }
        if (maximumRuinedPercentage != null) {
            return (int) Math.floor(maximumRuinedPercentage * entityCount);
        }
        return (int) Math.min(DEFAULT_MAXIMUM_RUINED_COUNT, entityCount);
    }

}
