package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "stageProviderClass",
        "entityClass",
        "variableName"
})
public class MultistageMoveSelectorConfig extends MoveSelectorConfig<MultistageMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "multistageMoveSelector";

    protected Class<?> stageProviderClass;

    protected Class<?> entityClass = null;
    protected String variableName = null;

    // **************************
    // Getters/Setters
    // **************************

    public Class<?> getStageProviderClass() {
        return stageProviderClass;
    }

    public void setStageProviderClass(
            Class<?> stageProviderClass) {
        this.stageProviderClass = stageProviderClass;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
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
        this.setStageProviderClass(stageProviderClass);
        return this;
    }

    public @NonNull MultistageMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
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
        classVisitor.accept(stageProviderClass);
    }

    @Override
    public @NonNull MultistageMoveSelectorConfig
            inherit(@NonNull MultistageMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        stageProviderClass =
                ConfigUtils.inheritOverwritableProperty(stageProviderClass,
                        inheritedConfig.getStageProviderClass());
        entityClass =
                ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }
}
