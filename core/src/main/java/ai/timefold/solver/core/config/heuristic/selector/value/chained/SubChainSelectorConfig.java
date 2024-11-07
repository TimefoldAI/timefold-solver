package ai.timefold.solver.core.config.heuristic.selector.value.chained;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "valueSelectorConfig",
        "minimumSubChainSize",
        "maximumSubChainSize"
})
public class SubChainSelectorConfig extends SelectorConfig<SubChainSelectorConfig> {

    @XmlElement(name = "valueSelector")
    protected ValueSelectorConfig valueSelectorConfig = null;

    protected Integer minimumSubChainSize = null;
    protected Integer maximumSubChainSize = null;

    public @Nullable ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(@Nullable ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public @Nullable Integer getMinimumSubChainSize() {
        return minimumSubChainSize;
    }

    public void setMinimumSubChainSize(@Nullable Integer minimumSubChainSize) {
        this.minimumSubChainSize = minimumSubChainSize;
    }

    public @Nullable Integer getMaximumSubChainSize() {
        return maximumSubChainSize;
    }

    public void setMaximumSubChainSize(@Nullable Integer maximumSubChainSize) {
        this.maximumSubChainSize = maximumSubChainSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SubChainSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public @NonNull SubChainSelectorConfig withMinimumSubChainSize(@NonNull Integer minimumSubChainSize) {
        this.setMinimumSubChainSize(minimumSubChainSize);
        return this;
    }

    public @NonNull SubChainSelectorConfig withMaximumSubChainSize(@NonNull Integer maximumSubChainSize) {
        this.setMaximumSubChainSize(maximumSubChainSize);
        return this;
    }

    @Override
    public @NonNull SubChainSelectorConfig inherit(@NonNull SubChainSelectorConfig inheritedConfig) {
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        minimumSubChainSize = ConfigUtils.inheritOverwritableProperty(minimumSubChainSize,
                inheritedConfig.getMinimumSubChainSize());
        maximumSubChainSize = ConfigUtils.inheritOverwritableProperty(maximumSubChainSize,
                inheritedConfig.getMaximumSubChainSize());
        return this;
    }

    @Override
    public @NonNull SubChainSelectorConfig copyConfig() {
        return new SubChainSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelectorConfig + ")";
    }

}
