package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.Objects;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "minimumRuinedCount",
        "maximumRuinedCount",
        "minimumRuinedPercentage",
        "maximumRuinedPercentage",
        "valueSelectorConfig",
        "constructionHeuristicConfig"
})
public class ListRuinMoveSelectorConfig extends MoveSelectorConfig<ListRuinMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "listRuinMoveSelector";

    protected Long minimumRuinedCount = null;
    protected Long maximumRuinedCount = null;

    protected Double minimumRuinedPercentage = null;
    protected Double maximumRuinedPercentage = null;

    @XmlElement(name = "valueSelector")
    protected ValueSelectorConfig valueSelectorConfig = null;

    @XmlElement(name = "constructionHeuristic")
    protected ConstructionHeuristicPhaseConfig constructionHeuristicConfig = null;

    public ListRuinMoveSelectorConfig() {

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

    public ListRuinMoveSelectorConfig withMinimumRuinedCount(Long minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
        return this;
    }

    public Long getMaximumRuinedCount() {
        return maximumRuinedCount;
    }

    public void setMaximumRuinedCount(Long maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
    }

    public ListRuinMoveSelectorConfig withMaximumRuinedCount(Long maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
        return this;
    }

    public Double getMinimumRuinedPercentage() {
        return minimumRuinedPercentage;
    }

    public void setMinimumRuinedPercentage(Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
    }

    public ListRuinMoveSelectorConfig withMinimumRuinedPercentage(Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
        return this;
    }

    public Double getMaximumRuinedPercentage() {
        return maximumRuinedPercentage;
    }

    public void setMaximumRuinedPercentage(Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
    }

    public ListRuinMoveSelectorConfig withMaximumRuinedPercentage(Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
        return this;
    }

    public ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(
            ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public ListRuinMoveSelectorConfig withValueSelectorConfig(ValueSelectorConfig entitySelectorConfig) {
        this.valueSelectorConfig = entitySelectorConfig;
        return this;
    }

    public ConstructionHeuristicPhaseConfig getConstructionHeuristicConfig() {
        return constructionHeuristicConfig;
    }

    public void setConstructionHeuristicConfig(
            ConstructionHeuristicPhaseConfig constructionHeuristicConfig) {
        this.constructionHeuristicConfig = constructionHeuristicConfig;
    }

    public ListRuinMoveSelectorConfig
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
    public ListRuinMoveSelectorConfig copyConfig() {
        return new ListRuinMoveSelectorConfig().inherit(this);
    }

    @Override
    public ListRuinMoveSelectorConfig inherit(ListRuinMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        constructionHeuristicConfig = ConfigUtils.inheritConfig(constructionHeuristicConfig,
                inheritedConfig.getConstructionHeuristicConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
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
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (constructionHeuristicConfig != null) {
            constructionHeuristicConfig.visitReferencedClasses(classVisitor);
        }
    }

    // **************************
    // Complex methods
    // **************************
    public ValueSelectorConfig determineValueSelectorConfig() {
        return Objects.requireNonNullElseGet(valueSelectorConfig, ValueSelectorConfig::new);
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
