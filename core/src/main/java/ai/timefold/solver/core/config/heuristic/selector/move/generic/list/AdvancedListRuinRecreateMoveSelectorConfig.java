package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "ruinAndRecreatePickerClass",
        "entityClass",
        "valueClass",
        "variableName"
})
public class AdvancedListRuinRecreateMoveSelectorConfig extends MoveSelectorConfig<AdvancedListRuinRecreateMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "advancedListRuinRecreateMoveSelector";

    protected Class<?> ruinAndRecreatePickerClass;

    protected Class<?> entityClass = null;
    protected Class<?> valueClass = null;
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

    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig withRuinAndRecreatePickerClass(
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

    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig withEntityClass(@NonNull Class<?> entityClass) {
        this.setEntityClass(entityClass);
        return this;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }

    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig withValueClass(@NonNull Class<?> valueClass) {
        this.setValueClass(valueClass);
        return this;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig withVariableName(@NonNull String variableName) {
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
    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig copyConfig() {
        return new AdvancedListRuinRecreateMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(ruinAndRecreatePickerClass);
    }

    @Override
    public @NonNull AdvancedListRuinRecreateMoveSelectorConfig
            inherit(@NonNull AdvancedListRuinRecreateMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        ruinAndRecreatePickerClass =
                ConfigUtils.inheritOverwritableProperty(ruinAndRecreatePickerClass,
                        inheritedConfig.getRuinAndRecreatePickerClass());
        entityClass =
                ConfigUtils.inheritOverwritableProperty(entityClass, inheritedConfig.getEntityClass());
        valueClass =
                ConfigUtils.inheritOverwritableProperty(valueClass, inheritedConfig.getValueClass());
        variableName =
                ConfigUtils.inheritOverwritableProperty(variableName, inheritedConfig.getVariableName());
        return this;
    }
}
