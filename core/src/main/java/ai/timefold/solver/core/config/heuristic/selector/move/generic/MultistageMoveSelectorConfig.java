package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "stageProviderClass",
        "entityClass",
        "variableName"
})
public final class MultistageMoveSelectorConfig extends MoveSelectorConfig<MultistageMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "multistageMoveSelector";

    private String stageProviderClass;

    private String entityClass = null;
    private String variableName = null;

    // **************************
    // Getters/Setters
    // **************************

    public @Nullable Class<?> getStageProviderClass() {
        return ConfigUtils.resolveClass(stageProviderClass, "stageProviderClass", this);
    }

    public void setStageProviderClass(Class<?> stageProviderClass) {
        this.stageProviderClass = stageProviderClass == null ? null : stageProviderClass.getName();
    }

    public @Nullable Class<?> getEntityClass() {
        return ConfigUtils.resolveClass(entityClass, "entityClass", this);
    }

    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass == null ? null : entityClass.getName();
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    // **************************
    // With methods
    // **************************

    public @NonNull MultistageMoveSelectorConfig withStageProviderClass(
            @NonNull Class<?> stageProviderClass) {
        this.stageProviderClass = stageProviderClass.getName();
        return this;
    }

    public @NonNull MultistageMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.entityClass = entityClass.getName();
        return this;
    }

    public @NonNull MultistageMoveSelectorConfig withVariableName(@NonNull String variableName) {
        this.setVariableName(variableName);
        return this;
    }

    // **************************
    // Interface methods
    // **************************

    @Override
    public boolean hasNearbySelectionConfig() {
        return false;
    }

    @Override
    public @NonNull MultistageMoveSelectorConfig copyConfig() {
        return new MultistageMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(getStageProviderClass());
    }

    @Override
    public @NonNull MultistageMoveSelectorConfig
            inherit(@NonNull MultistageMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        stageProviderClass =
                ConfigUtils.inheritOverwritableProperty(stageProviderClass,
                        inheritedConfig.stageProviderClass);
        entityClass =
                ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.entityClass);
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }
}
