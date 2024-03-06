package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

@XmlType(propOrder = {
        "valueSelectorConfig",
        "destinationSelectorConfig"
})
public class ListChangeMoveSelectorConfig extends MoveSelectorConfig<ListChangeMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "listChangeMoveSelector";

    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    @XmlElement(name = "destinationSelector")
    private DestinationSelectorConfig destinationSelectorConfig = null;

    public ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public DestinationSelectorConfig getDestinationSelectorConfig() {
        return destinationSelectorConfig;
    }

    public void setDestinationSelectorConfig(DestinationSelectorConfig destinationSelectorConfig) {
        this.destinationSelectorConfig = destinationSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public ListChangeMoveSelectorConfig withValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public ListChangeMoveSelectorConfig withDestinationSelectorConfig(DestinationSelectorConfig destinationSelectorConfig) {
        this.setDestinationSelectorConfig(destinationSelectorConfig);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public ListChangeMoveSelectorConfig inherit(ListChangeMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        destinationSelectorConfig =
                ConfigUtils.inheritConfig(destinationSelectorConfig, inheritedConfig.getDestinationSelectorConfig());
        return this;
    }

    @Override
    public ListChangeMoveSelectorConfig copyConfig() {
        return new ListChangeMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (destinationSelectorConfig != null) {
            destinationSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public ListChangeMoveSelectorConfig enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random) {
        ListChangeMoveSelectorConfig nearbyConfig = copyConfig();
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        String valueSelectorId = addRandomSuffix("valueSelector", random);
        valueConfig.withId(valueSelectorId);
        DestinationSelectorConfig destinationConfig = nearbyConfig.getDestinationSelectorConfig();
        if (destinationConfig == null) {
            destinationConfig = new DestinationSelectorConfig();
        }
        destinationConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(valueSelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withDestinationSelectorConfig(destinationConfig);
        return nearbyConfig;
    }

    @Override
    public boolean acceptNearbySelectionAutoConfiguration() {
        return true;
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (valueSelectorConfig != null && valueSelectorConfig.getNearbySelectionConfig() != null)
                || (destinationSelectorConfig != null && ((destinationSelectorConfig.getNearbySelectionConfig() != null)
                        || (destinationSelectorConfig.getEntitySelectorConfig() != null
                                && destinationSelectorConfig.getEntitySelectorConfig().getNearbySelectionConfig() != null)
                        || (destinationSelectorConfig.getValueSelectorConfig() != null
                                && destinationSelectorConfig.getValueSelectorConfig().getNearbySelectionConfig() != null)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelectorConfig + ", " + destinationSelectorConfig + ")";
    }
}
