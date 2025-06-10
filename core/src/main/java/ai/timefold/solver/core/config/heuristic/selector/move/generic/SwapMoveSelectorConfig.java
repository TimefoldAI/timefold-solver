package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyUtil;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "entitySelectorConfig",
        "secondaryEntitySelectorConfig",
        "variableNameIncludeList"
})
public class SwapMoveSelectorConfig
        extends MoveSelectorConfig<SwapMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<SwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "swapMoveSelector";

    @XmlElement(name = "entitySelector")
    private EntitySelectorConfig entitySelectorConfig = null;
    @XmlElement(name = "secondaryEntitySelector")
    private EntitySelectorConfig secondaryEntitySelectorConfig = null;

    @XmlElementWrapper(name = "variableNameIncludes")
    @XmlElement(name = "variableNameInclude")
    private List<String> variableNameIncludeList = null;

    public @Nullable EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(@Nullable EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public @Nullable EntitySelectorConfig getSecondaryEntitySelectorConfig() {
        return secondaryEntitySelectorConfig;
    }

    public void setSecondaryEntitySelectorConfig(@Nullable EntitySelectorConfig secondaryEntitySelectorConfig) {
        this.secondaryEntitySelectorConfig = secondaryEntitySelectorConfig;
    }

    public @Nullable List<@NonNull String> getVariableNameIncludeList() {
        return variableNameIncludeList;
    }

    public void setVariableNameIncludeList(@Nullable List<@NonNull String> variableNameIncludeList) {
        this.variableNameIncludeList = variableNameIncludeList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SwapMoveSelectorConfig withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull SwapMoveSelectorConfig
            withSecondaryEntitySelectorConfig(@NonNull EntitySelectorConfig secondaryEntitySelectorConfig) {
        this.setSecondaryEntitySelectorConfig(secondaryEntitySelectorConfig);
        return this;
    }

    public @NonNull SwapMoveSelectorConfig withVariableNameIncludes(@NonNull String @NonNull... variableNameIncludes) {
        this.setVariableNameIncludeList(Arrays.asList(variableNameIncludes));
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull SwapMoveSelectorConfig inherit(@NonNull SwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        secondaryEntitySelectorConfig = ConfigUtils.inheritConfig(secondaryEntitySelectorConfig,
                inheritedConfig.getSecondaryEntitySelectorConfig());
        variableNameIncludeList = ConfigUtils.inheritMergeableListProperty(
                variableNameIncludeList, inheritedConfig.getVariableNameIncludeList());
        return this;
    }

    @Override
    public @NonNull SwapMoveSelectorConfig copyConfig() {
        return new SwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (secondaryEntitySelectorConfig != null) {
            secondaryEntitySelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public @NonNull SwapMoveSelectorConfig enableNearbySelection(
            @NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            @NonNull Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (entitySelectorConfig != null && entitySelectorConfig.hasNearbySelectionConfig())
                || (secondaryEntitySelectorConfig != null && secondaryEntitySelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public boolean canEnableNearbyInMixedModels() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig
                + (secondaryEntitySelectorConfig == null ? "" : ", " + secondaryEntitySelectorConfig) + ")";
    }

}
