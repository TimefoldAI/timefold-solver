package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "minimumRuinedCount",
        "maximumRuinedCount",
        "minimumRuinedPercentage",
        "maximumRuinedPercentage"
})
public class ListRuinMoveSelectorConfig extends MoveSelectorConfig<ListRuinMoveSelectorConfig> {

    // Determined by benchmarking on multiple datasets.
    private static final int DEFAULT_MINIMUM_RUINED_COUNT = 5;
    private static final int DEFAULT_MAXIMUM_RUINED_COUNT = 40;

    public static final String XML_ELEMENT_NAME = "listRuinMoveSelector";

    protected Long minimumRuinedCount = null;
    protected Long maximumRuinedCount = null;

    protected Double minimumRuinedPercentage = null;
    protected Double maximumRuinedPercentage = null;

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

    // **************************
    // Interface methods
    // **************************

    @Override
    public boolean hasNearbySelectionConfig() {
        return false;
    }

    @Override
    public ListRuinMoveSelectorConfig copyConfig() {
        return new ListRuinMoveSelectorConfig().inherit(this);
    }

    @Override
    public ListRuinMoveSelectorConfig inherit(ListRuinMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
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
        // No referenced classes.
    }

    public long determineMinimumRuinedCount(long entityCount) {
        if (minimumRuinedCount != null) {
            return minimumRuinedCount;
        }
        if (minimumRuinedPercentage != null) {
            return (long) Math.floor(minimumRuinedPercentage * entityCount);
        }
        return Math.min(DEFAULT_MINIMUM_RUINED_COUNT, entityCount);
    }

    public long determineMaximumRuinedCount(long entityCount) {
        if (maximumRuinedCount != null) {
            return maximumRuinedCount;
        }
        if (maximumRuinedPercentage != null) {
            return (long) Math.floor(maximumRuinedPercentage * entityCount);
        }
        return Math.min(DEFAULT_MAXIMUM_RUINED_COUNT, entityCount);
    }

}
