package ai.timefold.solver.core.config.heuristic.selector.move.generic.chained;

import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyUtil;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

/**
 * Also known as a 2-opt move selector config.
 */
@XmlType(propOrder = {
        "entitySelectorConfig",
        "valueSelectorConfig"
})
public class TailChainSwapMoveSelectorConfig
        extends MoveSelectorConfig<TailChainSwapMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<TailChainSwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "tailChainSwapMoveSelector";

    @XmlElement(name = "entitySelector")
    private EntitySelectorConfig entitySelectorConfig = null;
    /**
     * Uses a valueSelector instead of a secondaryEntitySelector because
     * the secondary entity might not exist if the value is a buoy (= the last entity in a chain)
     * and also because with nearby selection, it's more important that the value is near (instead of the secondary entity).
     */
    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    public EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
    }

    public ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public TailChainSwapMoveSelectorConfig withEntitySelectorConfig(EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
        return this;
    }

    public TailChainSwapMoveSelectorConfig withValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
        return this;
    }

    @Override
    public TailChainSwapMoveSelectorConfig inherit(TailChainSwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        return this;
    }

    @Override
    public TailChainSwapMoveSelectorConfig copyConfig() {
        return new TailChainSwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public TailChainSwapMoveSelectorConfig enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (entitySelectorConfig != null && entitySelectorConfig.hasNearbySelectionConfig())
                || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + entitySelectorConfig + ", " + valueSelectorConfig + ")";
    }

}
