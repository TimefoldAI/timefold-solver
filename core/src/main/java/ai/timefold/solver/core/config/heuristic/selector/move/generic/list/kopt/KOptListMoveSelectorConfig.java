package ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt;

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
        "minimumK",
        "maximumK",
        "originSelectorConfig",
        "valueSelectorConfig"
})
public class KOptListMoveSelectorConfig
        extends MoveSelectorConfig<KOptListMoveSelectorConfig>
        implements NearbyAutoConfigurationEnabled<KOptListMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "kOptListMoveSelector";

    protected Integer minimumK = null;
    protected Integer maximumK = null;

    @XmlElement(name = "originSelector")
    private ValueSelectorConfig originSelectorConfig = null;

    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    public Integer getMinimumK() {
        return minimumK;
    }

    public void setMinimumK(Integer minimumK) {
        this.minimumK = minimumK;
    }

    public Integer getMaximumK() {
        return maximumK;
    }

    public void setMaximumK(Integer maximumK) {
        this.maximumK = maximumK;
    }

    public ValueSelectorConfig getOriginSelectorConfig() {
        return originSelectorConfig;
    }

    public void setOriginSelectorConfig(ValueSelectorConfig originSelectorConfig) {
        this.originSelectorConfig = originSelectorConfig;
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

    public KOptListMoveSelectorConfig withMinimumK(Integer minimumK) {
        this.minimumK = minimumK;
        return this;
    }

    public KOptListMoveSelectorConfig withMaximumK(Integer maximumK) {
        this.maximumK = maximumK;
        return this;
    }

    public KOptListMoveSelectorConfig withOriginSelectorConfig(ValueSelectorConfig originSelectorConfig) {
        this.originSelectorConfig = originSelectorConfig;
        return this;
    }

    public KOptListMoveSelectorConfig withValueSelectorConfig(ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public KOptListMoveSelectorConfig inherit(KOptListMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        this.minimumK = ConfigUtils.inheritOverwritableProperty(minimumK, inheritedConfig.minimumK);
        this.maximumK = ConfigUtils.inheritOverwritableProperty(maximumK, inheritedConfig.maximumK);
        this.originSelectorConfig = ConfigUtils.inheritConfig(originSelectorConfig, inheritedConfig.originSelectorConfig);
        this.valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.valueSelectorConfig);
        return this;
    }

    @Override
    public KOptListMoveSelectorConfig copyConfig() {
        return new KOptListMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);

        if (originSelectorConfig != null) {
            originSelectorConfig.visitReferencedClasses(classVisitor);
        }

        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public KOptListMoveSelectorConfig enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (originSelectorConfig != null && originSelectorConfig.hasNearbySelectionConfig())
                || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "()";
    }
}
