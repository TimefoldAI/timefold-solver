package ai.timefold.solver.core.config.heuristic.selector.entity.pillar;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "entitySelectorConfig",
        "minimumSubPillarSize",
        "maximumSubPillarSize"
})
public class PillarSelectorConfig extends SelectorConfig<PillarSelectorConfig> {

    @XmlElement(name = "entitySelector")
    protected EntitySelectorConfig entitySelectorConfig = null;

    protected Integer minimumSubPillarSize = null;
    protected Integer maximumSubPillarSize = null;

    public @Nullable EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(@Nullable EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public @Nullable Integer getMinimumSubPillarSize() {
        return minimumSubPillarSize;
    }

    public void setMinimumSubPillarSize(@Nullable Integer minimumSubPillarSize) {
        this.minimumSubPillarSize = minimumSubPillarSize;
    }

    public @Nullable Integer getMaximumSubPillarSize() {
        return maximumSubPillarSize;
    }

    public void setMaximumSubPillarSize(@Nullable Integer maximumSubPillarSize) {
        this.maximumSubPillarSize = maximumSubPillarSize;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull PillarSelectorConfig withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull PillarSelectorConfig withMinimumSubPillarSize(@NonNull Integer minimumSubPillarSize) {
        this.setMinimumSubPillarSize(minimumSubPillarSize);
        return this;
    }

    public @NonNull PillarSelectorConfig withMaximumSubPillarSize(@NonNull Integer maximumSubPillarSize) {
        this.setMaximumSubPillarSize(maximumSubPillarSize);
        return this;
    }

    @Override
    public @NonNull PillarSelectorConfig inherit(@NonNull PillarSelectorConfig inheritedConfig) {
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        minimumSubPillarSize = ConfigUtils.inheritOverwritableProperty(minimumSubPillarSize,
                inheritedConfig.getMinimumSubPillarSize());
        maximumSubPillarSize = ConfigUtils.inheritOverwritableProperty(maximumSubPillarSize,
                inheritedConfig.getMaximumSubPillarSize());
        return this;
    }

    @Override
    public @NonNull PillarSelectorConfig copyConfig() {
        return new PillarSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig + ")";
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return entitySelectorConfig != null && entitySelectorConfig.hasNearbySelectionConfig();
    }
}
