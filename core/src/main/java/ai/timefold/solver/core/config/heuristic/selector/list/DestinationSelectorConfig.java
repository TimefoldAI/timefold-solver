package ai.timefold.solver.core.config.heuristic.selector.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "entitySelectorConfig",
        "valueSelectorConfig",
        "nearbySelectionConfig",
})
public class DestinationSelectorConfig extends SelectorConfig<DestinationSelectorConfig> {

    @XmlElement(name = "entitySelector")
    private EntitySelectorConfig entitySelectorConfig;
    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig;
    @XmlElement(name = "nearbySelection")
    private NearbySelectionConfig nearbySelectionConfig;

    public DestinationSelectorConfig() {
    }

    public DestinationSelectorConfig(@Nullable DestinationSelectorConfig inheritedConfig) {
        if (inheritedConfig != null) {
            inherit(inheritedConfig);
        }
    }

    public @Nullable EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(@Nullable EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
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

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull DestinationSelectorConfig withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull DestinationSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public @NonNull DestinationSelectorConfig withNearbySelectionConfig(@NonNull NearbySelectionConfig nearbySelectionConfig) {
        this.setNearbySelectionConfig(nearbySelectionConfig);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull DestinationSelectorConfig inherit(@NonNull DestinationSelectorConfig inheritedConfig) {
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        nearbySelectionConfig = ConfigUtils.inheritConfig(nearbySelectionConfig, inheritedConfig.getNearbySelectionConfig());
        return this;
    }

    @Override
    public @NonNull DestinationSelectorConfig copyConfig() {
        return new DestinationSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig + ", " + valueSelectorConfig + ")";
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return nearbySelectionConfig != null
                || (entitySelectorConfig != null && entitySelectorConfig.hasNearbySelectionConfig())
                || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }
}
