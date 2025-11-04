package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.preview.api.move.ruin.BasicRuinAndRecreatePicker;
import ai.timefold.solver.core.preview.api.move.ruin.BasicRuinAndRecreator;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "ruinAndRecreatePickerClass",
        "recreatorClass",
        "entityClass",
        "variableName"
})
public class AdvancedRuinRecreateMoveSelectorConfig extends MoveSelectorConfig<AdvancedRuinRecreateMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "advancedRuinRecreateMoveSelector";

    protected Class<? extends BasicRuinAndRecreatePicker<?, ?, ?, ?>> ruinAndRecreatePickerClass;
    protected Class<? extends BasicRuinAndRecreator<?, ?, ?, ?>> recreatorClass;

    protected Class<?> entityClass = null;
    protected String variableName = null;

    // **************************
    // Getters/Setters
    // **************************

    public Class<? extends BasicRuinAndRecreatePicker<?, ?, ?, ?>> getRuinAndRecreatePickerClass() {
        return ruinAndRecreatePickerClass;
    }

    public void setRuinAndRecreatePickerClass(
            Class<? extends BasicRuinAndRecreatePicker<?, ?, ?, ?>> ruinAndRecreatePickerClass) {
        this.ruinAndRecreatePickerClass = ruinAndRecreatePickerClass;
    }

    public @NonNull AdvancedRuinRecreateMoveSelectorConfig withRuinAndRecreatePickerClass(
            @NonNull Class<? extends BasicRuinAndRecreatePicker<?, ?, ?, ?>> ruinAndRecreatePickerClass) {
        this.setRuinAndRecreatePickerClass(ruinAndRecreatePickerClass);
        return this;
    }

    public Class<? extends BasicRuinAndRecreator<?, ?, ?, ?>> getRecreatorClass() {
        return recreatorClass;
    }

    public void setRecreatorClass(
            Class<? extends BasicRuinAndRecreator<?, ?, ?, ?>> recreatorClass) {
        this.recreatorClass = recreatorClass;
    }

    public @NonNull AdvancedRuinRecreateMoveSelectorConfig
            withRecreatorClass(@NonNull Class<? extends BasicRuinAndRecreator<?, ?, ?, ?>> recreatorClass) {
        this.setRecreatorClass(recreatorClass);
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
        classVisitor.accept(recreatorClass);
    }

    @Override
    public @NonNull AdvancedRuinRecreateMoveSelectorConfig
            inherit(@NonNull AdvancedRuinRecreateMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        ruinAndRecreatePickerClass =
                ConfigUtils.inheritOverwritableProperty(ruinAndRecreatePickerClass,
                        inheritedConfig.getRuinAndRecreatePickerClass());
        recreatorClass =
                ConfigUtils.inheritOverwritableProperty(recreatorClass, inheritedConfig.getRecreatorClass());
        entityClass =
                ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }
}
