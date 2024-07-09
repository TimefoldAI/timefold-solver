package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.Objects;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "minimumRuinedCount",
        "maximumRuinedCount",
        "minimumRuinedPercentage",
        "maximumRuinedPercentage",
        "entitySelectorConfig",
        "constructionHeuristicConfig"
})
public class RuinMoveSelectorConfig extends MoveSelectorConfig<RuinMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "ruinMoveSelector";

    protected Long minimumRuinedCount = null;
    protected Long maximumRuinedCount = null;

    protected Double minimumRuinedPercentage = null;
    protected Double maximumRuinedPercentage = null;

    @XmlElement(name = "entitySelector")
    protected EntitySelectorConfig entitySelectorConfig = null;

    @XmlElement(name = "constructionHeuristic")
    protected ConstructionHeuristicPhaseConfig constructionHeuristicConfig = null;

    public RuinMoveSelectorConfig() {

    }

    // **************************
    // Getters/Setters
    // **************************

    public Long getMinimumRuinedCount() {
        return minimumRuinedCount;
    }

    public void setMinimumRuinedCount(Long minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
    }

    public RuinMoveSelectorConfig withMinimumRuinedCount(Long minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
        return this;
    }

    public Long getMaximumRuinedCount() {
        return maximumRuinedCount;
    }

    public void setMaximumRuinedCount(Long maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
    }

    public RuinMoveSelectorConfig withMaximumRuinedCount(Long maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
        return this;
    }

    public Double getMinimumRuinedPercentage() {
        return minimumRuinedPercentage;
    }

    public void setMinimumRuinedPercentage(Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
    }

    public RuinMoveSelectorConfig withMinimumRuinedPercentage(Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
        return this;
    }

    public Double getMaximumRuinedPercentage() {
        return maximumRuinedPercentage;
    }

    public void setMaximumRuinedPercentage(Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
    }

    public RuinMoveSelectorConfig withMaximumRuinedPercentage(Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
        return this;
    }

    public EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(
            EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public RuinMoveSelectorConfig withEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
        return this;
    }

    public ConstructionHeuristicPhaseConfig getConstructionHeuristicConfig() {
        return constructionHeuristicConfig;
    }

    public void setConstructionHeuristicConfig(
            ConstructionHeuristicPhaseConfig constructionHeuristicConfig) {
        this.constructionHeuristicConfig = constructionHeuristicConfig;
    }

    public RuinMoveSelectorConfig
            withConstructionHeuristicConfig(ConstructionHeuristicPhaseConfig constructionHeuristicConfig) {
        this.constructionHeuristicConfig = constructionHeuristicConfig;
        return this;
    }

    // **************************
    // Interface methods
    // **************************

    @Override
    public boolean hasNearbySelectionConfig() {
        return constructionHeuristicConfig != null && constructionHeuristicConfig.getMoveSelectorConfigList()
                .stream().anyMatch(MoveSelectorConfig::hasNearbySelectionConfig);
    }

    @Override
    public RuinMoveSelectorConfig copyConfig() {
        return new RuinMoveSelectorConfig().inherit(this);
    }

    @Override
    public RuinMoveSelectorConfig inherit(RuinMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        constructionHeuristicConfig = ConfigUtils.inheritConfig(constructionHeuristicConfig,
                inheritedConfig.getConstructionHeuristicConfig());
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        minimumRuinedCount =
                ConfigUtils.inheritOverwritableProperty(minimumRuinedCount, inheritedConfig.getMinimumRuinedCount());
        maximumRuinedCount =
                ConfigUtils.inheritOverwritableProperty(maximumRuinedCount, inheritedConfig.getMaximumRuinedCount());
        minimumRuinedPercentage =
                ConfigUtils.inheritOverwritableProperty(minimumRuinedPercentage, inheritedConfig.getMinimumRuinedPercentage());
        maximumRuinedPercentage =
                ConfigUtils.inheritOverwritableProperty(maximumRuinedPercentage, inheritedConfig.getMaximumRuinedPercentage());
        return this;
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (constructionHeuristicConfig != null) {
            constructionHeuristicConfig.visitReferencedClasses(classVisitor);
        }
    }

    // **************************
    // Complex methods
    // **************************
    public EntitySelectorConfig determineEntitySelectorConfig() {
        return Objects.requireNonNullElseGet(entitySelectorConfig, EntitySelectorConfig::new);
    }

    public long determineMinimumRuinedCount(long entityCount) {
        if (minimumRuinedCount != null) {
            return minimumRuinedCount;
        }
        if (minimumRuinedPercentage != null) {
            return (long) Math.floor(minimumRuinedPercentage * entityCount);
        }
        return 1L;
    }

    public long determineMaximumRuinedCount(long entityCount) {
        if (maximumRuinedCount != null) {
            return maximumRuinedCount;
        }
        if (maximumRuinedPercentage != null) {
            return (long) Math.floor(maximumRuinedPercentage * entityCount);
        }
        return entityCount;
    }

    public ConstructionHeuristicPhaseConfig determineConstructionHeuristicConfig() {
        return Objects.requireNonNullElseGet(constructionHeuristicConfig, ConstructionHeuristicPhaseConfig::new);
    }
}
