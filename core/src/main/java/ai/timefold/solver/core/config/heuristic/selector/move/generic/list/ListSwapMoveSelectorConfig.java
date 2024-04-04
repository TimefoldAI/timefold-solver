package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyUtil;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

@XmlType(propOrder = {
        "valueSelectorConfig",
        "secondaryValueSelectorConfig"
})
public class ListSwapMoveSelectorConfig
        extends MoveSelectorConfig<ListSwapMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<ListSwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "listSwapMoveSelector";

    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;
    @XmlElement(name = "secondaryValueSelector")
    private ValueSelectorConfig secondaryValueSelectorConfig = null;

    public ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public ValueSelectorConfig getSecondaryValueSelectorConfig() {
        return secondaryValueSelectorConfig;
    }

    public void setSecondaryValueSelectorConfig(ValueSelectorConfig secondaryValueSelectorConfig) {
        this.secondaryValueSelectorConfig = secondaryValueSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public ListSwapMoveSelectorConfig withValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public ListSwapMoveSelectorConfig withSecondaryValueSelectorConfig(ValueSelectorConfig secondaryValueSelectorConfig) {
        this.setSecondaryValueSelectorConfig(secondaryValueSelectorConfig);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public ListSwapMoveSelectorConfig inherit(ListSwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        secondaryValueSelectorConfig = ConfigUtils.inheritConfig(secondaryValueSelectorConfig,
                inheritedConfig.getSecondaryValueSelectorConfig());
        return this;
    }

    @Override
    public ListSwapMoveSelectorConfig copyConfig() {
        return new ListSwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (secondaryValueSelectorConfig != null) {
            secondaryValueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public ListSwapMoveSelectorConfig enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig())
                || (secondaryValueSelectorConfig != null && secondaryValueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelectorConfig
                + (secondaryValueSelectorConfig == null ? "" : ", " + secondaryValueSelectorConfig) + ")";
    }

}
