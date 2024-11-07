package ai.timefold.solver.core.config.heuristic.selector.move.generic.chained;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "entityClass",
        "subChainSelectorConfig",
        "secondarySubChainSelectorConfig",
        "selectReversingMoveToo"
})
public class SubChainSwapMoveSelectorConfig extends MoveSelectorConfig<SubChainSwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "subChainSwapMoveSelector";

    private Class<?> entityClass = null;
    @XmlElement(name = "subChainSelector")
    private SubChainSelectorConfig subChainSelectorConfig = null;
    @XmlElement(name = "secondarySubChainSelector")
    private SubChainSelectorConfig secondarySubChainSelectorConfig = null;

    private Boolean selectReversingMoveToo = null;

    public @Nullable Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(@Nullable Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public @Nullable SubChainSelectorConfig getSubChainSelectorConfig() {
        return subChainSelectorConfig;
    }

    public void setSubChainSelectorConfig(@Nullable SubChainSelectorConfig subChainSelectorConfig) {
        this.subChainSelectorConfig = subChainSelectorConfig;
    }

    public @Nullable SubChainSelectorConfig getSecondarySubChainSelectorConfig() {
        return secondarySubChainSelectorConfig;
    }

    public void setSecondarySubChainSelectorConfig(@Nullable SubChainSelectorConfig secondarySubChainSelectorConfig) {
        this.secondarySubChainSelectorConfig = secondarySubChainSelectorConfig;
    }

    public @Nullable Boolean getSelectReversingMoveToo() {
        return selectReversingMoveToo;
    }

    public void setSelectReversingMoveToo(@Nullable Boolean selectReversingMoveToo) {
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull SubChainSwapMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public @NonNull SubChainSwapMoveSelectorConfig
            withSubChainSelectorConfig(@NonNull SubChainSelectorConfig subChainSelectorConfig) {
        this.setSubChainSelectorConfig(subChainSelectorConfig);
        return this;
    }

    public @NonNull SubChainSwapMoveSelectorConfig
            withSecondarySubChainSelectorConfig(@NonNull SubChainSelectorConfig secondarySubChainSelectorConfig) {
        this.setSecondarySubChainSelectorConfig(secondarySubChainSelectorConfig);
        return this;
    }

    public @NonNull SubChainSwapMoveSelectorConfig withSelectReversingMoveToo(@NonNull Boolean selectReversingMoveToo) {
        this.setSelectReversingMoveToo(selectReversingMoveToo);
        return this;
    }

    @Override
    public @NonNull SubChainSwapMoveSelectorConfig inherit(@NonNull SubChainSwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entityClass = ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        subChainSelectorConfig = ConfigUtils.inheritConfig(subChainSelectorConfig, inheritedConfig.getSubChainSelectorConfig());
        secondarySubChainSelectorConfig = ConfigUtils.inheritConfig(secondarySubChainSelectorConfig,
                inheritedConfig.getSecondarySubChainSelectorConfig());
        selectReversingMoveToo = ConfigUtils.inheritOverwritableProperty(selectReversingMoveToo,
                inheritedConfig.getSelectReversingMoveToo());
        return this;
    }

    @Override
    public @NonNull SubChainSwapMoveSelectorConfig copyConfig() {
        return new SubChainSwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        classVisitor.accept(entityClass);
        if (subChainSelectorConfig != null) {
            subChainSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (secondarySubChainSelectorConfig != null) {
            secondarySubChainSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (subChainSelectorConfig != null && subChainSelectorConfig.hasNearbySelectionConfig())
                || (secondarySubChainSelectorConfig != null && secondarySubChainSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + subChainSelectorConfig
                + (secondarySubChainSelectorConfig == null ? "" : ", " + secondarySubChainSelectorConfig) + ")";
    }

}
