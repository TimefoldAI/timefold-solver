package ai.timefold.solver.core.config.heuristic.selector.move.generic.chained;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * THIS CLASS IS EXPERIMENTAL AND UNSUPPORTED.
 * Backward compatibility is not guaranteed.
 * It's NOT DOCUMENTED because we'll only document it when it actually works in more than 1 use case.
 *
 * Do not use.
 *
 * @see TailChainSwapMoveSelectorConfig
 */
@XmlType(propOrder = {
        "entitySelectorConfig",
        "valueSelectorConfig"
})
public class KOptMoveSelectorConfig extends MoveSelectorConfig<KOptMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "kOptMoveSelector";

    @XmlElement(name = "entitySelector")
    private EntitySelectorConfig entitySelectorConfig = null;
    /**
     * Like {@link TailChainSwapMoveSelectorConfig#valueSelectorConfig} but used multiple times to create 1 move.
     */
    @XmlElement(name = "valueSelector")
    private ValueSelectorConfig valueSelectorConfig = null;

    public @Nullable EntitySelectorConfig getEntitySelectorConfig() {
        return entitySelectorConfig;
    }

    public void setEntitySelectorConfig(@Nullable EntitySelectorConfig entitySelectorConfig) {
        this.entitySelectorConfig = entitySelectorConfig;
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

    public @NonNull KOptMoveSelectorConfig withEntitySelectorConfig(@NonNull EntitySelectorConfig entitySelectorConfig) {
        this.setEntitySelectorConfig(entitySelectorConfig);
        return this;
    }

    public @NonNull KOptMoveSelectorConfig withValueSelectorConfig(@NonNull ValueSelectorConfig valueSelectorConfig) {
        this.setValueSelectorConfig(valueSelectorConfig);
        return this;
    }

    @Override
    public @NonNull KOptMoveSelectorConfig inherit(@NonNull KOptMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        entitySelectorConfig = ConfigUtils.inheritConfig(entitySelectorConfig, inheritedConfig.getEntitySelectorConfig());
        valueSelectorConfig = ConfigUtils.inheritConfig(valueSelectorConfig, inheritedConfig.getValueSelectorConfig());
        return this;
    }

    @Override
    public @NonNull KOptMoveSelectorConfig copyConfig() {
        return new KOptMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (entitySelectorConfig != null) {
            entitySelectorConfig.visitReferencedClasses(classVisitor);
        }
        if (valueSelectorConfig != null) {
            valueSelectorConfig.visitReferencedClasses(classVisitor);
        }
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
