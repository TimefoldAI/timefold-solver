package ai.timefold.solver.core.config.heuristic.selector.move.generic.chained;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "entityClass",
        "subChainSelectorConfig",
        "valueSelectorConfig",
        "selectReversingMoveToo"
})
public class SubChainChangeMoveSelectorConfig extends MoveSelectorConfig<SubChainChangeMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "subChainChangeMoveSelector";

    private Class<?> entityClass = null;
    @XmlElement(name = "subChainSelector")
    private SubChainSelectorConfig subChainSelectorConfig = null;
    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

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

    public @Nullable ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(@Nullable ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
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

    public @NonNull SubChainChangeMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public @NonNull SubChainChangeMoveSelectorConfig
            withSubChainSelectorConfig(@NonNull SubChainSelectorConfig subChainSelectorConfig) {
        this.setSubChainSelectorConfig(subChainSelectorConfig);
        return this;
    }

    public @NonNull SubChainChangeMoveSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public @NonNull SubChainChangeMoveSelectorConfig withSelectReversingMoveToo(@NonNull Boolean selectReversingMoveToo) {
        this.setSelectReversingMoveToo(selectReversingMoveToo);
        return this;
    }

    @Override
    public @NonNull SubChainChangeMoveSelectorConfig inherit(@NonNull SubChainChangeMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entityClass = ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        subChainSelectorConfig = ConfigUtils.inheritConfig(subChainSelectorConfig, inheritedConfig.getSubChainSelectorConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        selectReversingMoveToo = ConfigUtils.inheritOverwritableProperty(selectReversingMoveToo,
                inheritedConfig.getSelectReversingMoveToo());
        return this;
    }

    @Override
    public @NonNull SubChainChangeMoveSelectorConfig copyConfig() {
        return new SubChainChangeMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        classVisitor.accept(entityClass);
        if (subChainSelectorConfig != null) {
            subChainSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (subChainSelectorConfig != null && subChainSelectorConfig.hasNearbySelectionConfig())
                || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + subChainSelectorConfig + ", " + valueSelectorConfig + ")";
    }

}
