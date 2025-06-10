package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.Random;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyUtil;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "valueSelectorConfig",
        "destinationSelectorConfig"
})
public class ListChangeMoveSelectorConfig
        extends MoveSelectorConfig<ListChangeMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<ListChangeMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "listChangeMoveSelector";

    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    @XmlElement(name = "destinationSelector")
    private DestinationSelectorConfig destinationSelectorConfig = null;

    public @Nullable ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(@Nullable ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    public @Nullable DestinationSelectorConfig getDestinationSelectorConfig() {
        return destinationSelectorConfig;
    }

    public void setDestinationSelectorConfig(@Nullable DestinationSelectorConfig destinationSelectorConfig) {
        this.destinationSelectorConfig = destinationSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull ListChangeMoveSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    public @NonNull ListChangeMoveSelectorConfig
            withDestinationSelectorConfig(@NonNull DestinationSelectorConfig destinationSelectorConfig) {
        this.setDestinationSelectorConfig(destinationSelectorConfig);
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull ListChangeMoveSelectorConfig inherit(@NonNull ListChangeMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        destinationSelectorConfig =
                ConfigUtils.inheritConfig(destinationSelectorConfig, inheritedConfig.getDestinationSelectorConfig());
        return this;
    }

    @Override
    public @NonNull ListChangeMoveSelectorConfig copyConfig() {
        return new ListChangeMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (destinationSelectorConfig != null) {
            destinationSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public @NonNull ListChangeMoveSelectorConfig enableNearbySelection(
            @NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            @NonNull Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig())
                || (destinationSelectorConfig != null && destinationSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public boolean canEnableNearbyInMixedModels() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + valueSelectorConfig + ", " + destinationSelectorConfig + ")";
    }

}
