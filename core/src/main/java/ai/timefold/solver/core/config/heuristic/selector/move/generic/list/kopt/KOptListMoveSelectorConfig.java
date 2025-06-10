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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    public @Nullable Integer getMinimumK() {
        return minimumK;
    }

    public void setMinimumK(@Nullable Integer minimumK) {
        this.minimumK = minimumK;
    }

    public @Nullable Integer getMaximumK() {
        return maximumK;
    }

    public void setMaximumK(@Nullable Integer maximumK) {
        this.maximumK = maximumK;
    }

    public @Nullable ValueSelectorConfig getOriginSelectorConfig() {
        return originSelectorConfig;
    }

    public void setOriginSelectorConfig(@Nullable ValueSelectorConfig originSelectorConfig) {
        this.originSelectorConfig = originSelectorConfig;
    }

    public @Nullable ValueSelectorConfig getValueSelectorConfig() {
        return valueSelectorConfig;
    }

    public void setValueSelectorConfig(@Nullable ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull KOptListMoveSelectorConfig withMinimumK(@NonNull Integer minimumK) {
        this.minimumK = minimumK;
        return this;
    }

    public @NonNull KOptListMoveSelectorConfig withMaximumK(@NonNull Integer maximumK) {
        this.maximumK = maximumK;
        return this;
    }

    public @NonNull KOptListMoveSelectorConfig withOriginSelectorConfig(@NonNull ValueSelectorConfig originSelectorConfig) {
        this.originSelectorConfig = originSelectorConfig;
        return this;
    }

    public @NonNull KOptListMoveSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.valueSelectorConfig = valueSelectorConfig;
        return this;
    }

    // ************************************************************************
    // Builder methods
    // ************************************************************************

    @Override
    public @NonNull KOptListMoveSelectorConfig inherit(@NonNull KOptListMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        this.minimumK = ConfigUtils.inheritOverwritableProperty(minimumK, inheritedConfig.minimumK);
        this.maximumK = ConfigUtils.inheritOverwritableProperty(maximumK, inheritedConfig.maximumK);
        this.originSelectorConfig = ConfigUtils.inheritConfig(originSelectorConfig, inheritedConfig.originSelectorConfig);
        this.valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.valueSelectorConfig);
        return this;
    }

    @Override
    public @NonNull KOptListMoveSelectorConfig copyConfig() {
        return new KOptListMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);

        if (originSelectorConfig != null) {
            originSelectorConfig.visitReferencedClasses(classVisitor);
        }

        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public @NonNull KOptListMoveSelectorConfig enableNearbySelection(
            @NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            @NonNull Random random) {
        return NearbyUtil.enable(this, distanceMeter, random);
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (originSelectorConfig != null && originSelectorConfig.hasNearbySelectionConfig())
                || (valueSelectorConfig != null && valueSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public boolean canEnableNearbyInMixedModels() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "()";
    }
}
