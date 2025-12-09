package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "ruinAndRecreatePickerClass",
        "entityClass",
        "variableName"
})
public class AdvancedRuinRecreateMoveSelectorConfig extends MoveSelectorConfig<AdvancedRuinRecreateMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "advancedRuinRecreateMoveSelector";

    protected Class<?> ruinAndRecreatePickerClass;

    protected Class<?> entityClass = null;
    protected String variableName = null;

    // **************************
    // Getters/Setters
    // **************************

    public Class<?> getRuinAndRecreatePickerClass() {
        return ruinAndRecreatePickerClass;
    }

    public void setRuinAndRecreatePickerClass(
            Class<?> ruinAndRecreatePickerClass) {
        this.ruinAndRecreatePickerClass = ruinAndRecreatePickerClass;
    }

    public @NonNull AdvancedRuinRecreateMoveSelectorConfig withRuinAndRecreatePickerClass(
            @NonNull Class<?> ruinAndRecreatePickerClass) {
        this.setRuinAndRecreatePickerClass(ruinAndRecreatePickerClass);
        return this;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public @NonNull AdvancedRuinRecreateMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public @NonNull AdvancedRuinRecreateMoveSelectorConfig withVariableName(@NonNull String variableName) {
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
    public @NonNull AdvancedRuinRecreateMoveSelectorConfig copyConfig() {
        return new AdvancedRuinRecreateMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(ruinAndRecreatePickerClass);
    }

    @Override
    public @NonNull AdvancedRuinRecreateMoveSelectorConfig
            inherit(@NonNull AdvancedRuinRecreateMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        ruinAndRecreatePickerClass =
                ConfigUtils.inheritOverwritableProperty(ruinAndRecreatePickerClass,
                        inheritedConfig.getRuinAndRecreatePickerClass());
        entityClass =
                ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }
}
