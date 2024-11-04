package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "minimumRuinedCount",
        "maximumRuinedCount",
        "minimumRuinedPercentage",
        "maximumRuinedPercentage"
})
public class ListRuinRecreateMoveSelectorConfig extends MoveSelectorConfig<ListRuinRecreateMoveSelectorConfig> {

    // Determined by benchmarking on multiple datasets.
    private static final int DEFAULT_MINIMUM_RUINED_COUNT = 5;
    private static final int DEFAULT_MAXIMUM_RUINED_COUNT = 40;

    public static final String XML_ELEMENT_NAME = "listRuinRecreateMoveSelector";

    protected Integer minimumRuinedCount = null;
    protected Integer maximumRuinedCount = null;

    protected Double minimumRuinedPercentage = null;
    protected Double maximumRuinedPercentage = null;

    // **************************
    // Getters/Setters
    // **************************

    public @Nullable Integer getMinimumRuinedCount() {
        return minimumRuinedCount;
    }

    public void setMinimumRuinedCount(@Nullable Integer minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
    }

    public @NonNull ListRuinRecreateMoveSelectorConfig withMinimumRuinedCount(@NonNull Integer minimumRuinedCount) {
        this.minimumRuinedCount = minimumRuinedCount;
        return this;
    }

    public @Nullable Integer getMaximumRuinedCount() {
        return maximumRuinedCount;
    }

    public void setMaximumRuinedCount(@Nullable Integer maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
    }

    public @NonNull ListRuinRecreateMoveSelectorConfig withMaximumRuinedCount(@NonNull Integer maximumRuinedCount) {
        this.maximumRuinedCount = maximumRuinedCount;
        return this;
    }

    public @Nullable Double getMinimumRuinedPercentage() {
        return minimumRuinedPercentage;
    }

    public void setMinimumRuinedPercentage(@Nullable Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
    }

    public @NonNull ListRuinRecreateMoveSelectorConfig withMinimumRuinedPercentage(@NonNull Double minimumRuinedPercentage) {
        this.minimumRuinedPercentage = minimumRuinedPercentage;
        return this;
    }

    public @Nullable Double getMaximumRuinedPercentage() {
        return maximumRuinedPercentage;
    }

    public void setMaximumRuinedPercentage(@Nullable Double maximumRuinedPercentage) {
        this.maximumRuinedPercentage = maximumRuinedPercentage;
    }

    public @NonNull ListRuinRecreateMoveSelectorConfig withMaximumRuinedPercentage(@NonNull Double maximumRuinedPercentage) {
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
    public @NonNull ListRuinRecreateMoveSelectorConfig copyConfig() {
        return new ListRuinRecreateMoveSelectorConfig().inherit(this);
    }

    @Override
    public @NonNull ListRuinRecreateMoveSelectorConfig inherit(@NonNull ListRuinRecreateMoveSelectorConfig inheritedConfig) {
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
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        // No referenced classes.
    }

    public int determineMinimumRuinedCount(long valueCount) {
        if (minimumRuinedCount != null) {
            return minimumRuinedCount;
        }
        if (minimumRuinedPercentage != null) {
            return (int) Math.floor(minimumRuinedPercentage * valueCount);
        }
        return (int) Math.min(DEFAULT_MINIMUM_RUINED_COUNT, valueCount);
    }

    public int determineMaximumRuinedCount(long valueCount) {
        if (maximumRuinedCount != null) {
            return maximumRuinedCount;
        }
        if (maximumRuinedPercentage != null) {
            return (int) Math.floor(maximumRuinedPercentage * valueCount);
        }
        return (int) Math.min(DEFAULT_MAXIMUM_RUINED_COUNT, valueCount);
    }

}
