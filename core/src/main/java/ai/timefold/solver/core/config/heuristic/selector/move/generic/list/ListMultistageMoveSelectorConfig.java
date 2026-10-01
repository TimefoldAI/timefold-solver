package ai.timefold.solver.core.config.heuristic.selector.move.generic.list;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "stageProviderClass"
})
public final class ListMultistageMoveSelectorConfig extends MoveSelectorConfig<ListMultistageMoveSelectorConfig> {
    public static final String XML_ELEMENT_NAME = "listMultistageMoveSelector";

    private String stageProviderClass;

    // **************************
    // Getters/Setters
    // **************************

    public @Nullable Class<?> getStageProviderClass() {
        return ConfigUtils.resolveClass(stageProviderClass, "stageProviderClass", this);
    }

    public void setStageProviderClass(Class<?> stageProviderClass) {
        this.stageProviderClass = stageProviderClass == null ? null : stageProviderClass.getName();
    }

    // **************************
    // With methods
    // **************************

    public @NonNull ListMultistageMoveSelectorConfig withStageProviderClass(
            @NonNull Class<?> stageProviderClass) {
        this.stageProviderClass = stageProviderClass.getName();
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
        classVisitor.accept(getStageProviderClass());
    }

    @Override
    public @NonNull ListMultistageMoveSelectorConfig
            inherit(@NonNull ListMultistageMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        stageProviderClass =
                ConfigUtils.inheritOverwritableProperty(stageProviderClass,
                        inheritedConfig.stageProviderClass);
        return this;
    }
}
