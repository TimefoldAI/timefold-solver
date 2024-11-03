package ai.timefold.solver.core.config.heuristic.selector.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "id",
        "mimicSelectorRef",
        "valueSelectorConfig",
        "nearbySelectionConfig",
        "minimumSubListSize",
        "maximumSubListSize",
})
public class SubListSelectorConfig extends SelectorConfig<SubListSelectorConfig> {

    @XmlAttribute
    private String id = null;
    @XmlAttribute
    private String mimicSelectorRef = null;

    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;
    @XmlElement(name = "nearbySelection")
    private NearbySelectionConfig nearbySelectionConfig = null;

    private Integer minimumSubListSize = null;
    private Integer maximumSubListSize = null;

    public SubListSelectorConfig() {
    }

    public SubListSelectorConfig(@Nullable SubListSelectorConfig inheritedConfig) {
        if (inheritedConfig != null) {
            inherit(inheritedConfig);
        }
    }

    public @Nullable String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @Nullable String getMimicSelectorRef() {
        return mimicSelectorRef;
    }

    public void setMimicSelectorRef(@Nullable String mimicSelectorRef) {
        this.mimicSelectorRef = mimicSelectorRef;
    }

    public @Nullable ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(@Nullable ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public @Nullable NearbySelectionConfig getNearbySelectionConfig() {
        return nearbySelectionConfig;
    }

    public void setNearbySelectionConfig(@Nullable NearbySelectionConfig nearbySelectionConfig) {
        this.nearbySelectionConfig = nearbySelectionConfig;
    }

    public @Nullable Integer getMinimumSubListSize() {
        return minimumSubListSize;
    }

    public void setMinimumSubListSize(@Nullable Integer minimumSubListSize) {
        this.minimumSubListSize = minimumSubListSize;
    }

    public @Nullable Integer getMaximumSubListSize() {
        return maximumSubListSize;
    }

    public void setMaximumSubListSize(@Nullable Integer maximumSubListSize) {
        this.maximumSubListSize = maximumSubListSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SubListSelectorConfig withId(@NonNull String id) {
        this.setId(id);
        return this;
    }

    public @NonNull SubListSelectorConfig withMimicSelectorRef(@NonNull String mimicSelectorRef) {
        this.setMimicSelectorRef(mimicSelectorRef);
        return this;
    }

    public @NonNull SubListSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public @NonNull SubListSelectorConfig withNearbySelectionConfig(@NonNull NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    public @NonNull SubListSelectorConfig withMinimumSubListSize(@NonNull Integer minimumSubListSize) {
        this.setMinimumSubListSize(minimumSubListSize);
        return this;
    }

    public @NonNull SubListSelectorConfig withMaximumSubListSize(@NonNull Integer maximumSubListSize) {
        this.setMaximumSubListSize(maximumSubListSize);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull SubListSelectorConfig inherit(@NonNull SubListSelectorConfig inheritedConfig) {
        id = ConfigUtils.inheritOverwritableProperty(id, inheritedConfig.id);
        mimicSelectorRef = ConfigUtils.inheritOverwritableProperty(mimicSelectorRef, inheritedConfig.mimicSelectorRef);
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.valueSelectorConfig);
        nearbySelectionConfig = ConfigUtils.inheritConfig(nearbySelectionConfig, inheritedConfig.nearbySelectionConfig);
        minimumSubListSize = ConfigUtils.inheritOverwritableProperty(minimumSubListSize, inheritedConfig.minimumSubListSize);
        maximumSubListSize = ConfigUtils.inheritOverwritableProperty(maximumSubListSize, inheritedConfig.maximumSubListSize);
        return this;
    }

    @Override
    public @NonNull SubListSelectorConfig copyConfig() {
        return new SubListSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return nearbySelectionConfig != null || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelectorConfig + ")";
    }
}
