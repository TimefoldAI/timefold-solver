package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;

@XmlType(propOrder = {
        "stageProviderClass"
})
public class ListMultistageMoveSelectorConfig extends MoveSelectorConfig<ListMultistageMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "listMultistageMoveSelector";

    protected Class<?> stageProviderClass;

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

    public @NonNull ListMultistageMoveSelectorConfig withStageProviderClass(
            @NonNull Class<?> stageProviderClass) {
        this.setStageProviderClass(stageProviderClass);
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
    public @NonNull ListMultistageMoveSelectorConfig copyConfig() {
        return new ListMultistageMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        classVisitor.accept(stageProviderClass);
    }

    @Override
    public @NonNull ListMultistageMoveSelectorConfig
            inherit(@NonNull ListMultistageMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        stageProviderClass =
                ConfigUtils.inheritOverwritableProperty(stageProviderClass,
                        inheritedConfig.getStageProviderClass());
        return this;
    }
}
