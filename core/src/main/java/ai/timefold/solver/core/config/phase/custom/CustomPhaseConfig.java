package ai.timefold.solver.core.config.phase.custom;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "customPhaseCommandClassList",
        "customProperties",
})
public class CustomPhaseConfig extends PhaseConfig<CustomPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "customPhase";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    @XmlElement(name = "customPhaseCommandClass")
    protected List<Class<? extends CustomPhaseCommand>> customPhaseCommandClassList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected Map<String, String> customProperties = null;

    @XmlTransient
    protected List<CustomPhaseCommand> customPhaseCommandList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable List<Class<? extends CustomPhaseCommand>> getCustomPhaseCommandClassList() {
        return customPhaseCommandClassList;
    }

    public void setCustomPhaseCommandClassList(
            @Nullable List<Class<? extends CustomPhaseCommand>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
    }

    public @Nullable Map<@NonNull String, @NonNull String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(@Nullable Map<@NonNull String, @NonNull String> customProperties) {
        this.customProperties = customProperties;
    }

    public @Nullable List<@NonNull CustomPhaseCommand> getCustomPhaseCommandList() {
        return customPhaseCommandList;
    }

    public void setCustomPhaseCommandList(@Nullable List<@NonNull CustomPhaseCommand> customPhaseCommandList) {
        this.customPhaseCommandList = customPhaseCommandList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull CustomPhaseConfig withCustomPhaseCommandClassList(
            @NonNull List<@NonNull Class<? extends CustomPhaseCommand>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
        return this;
    }

    public @NonNull CustomPhaseConfig withCustomProperties(@NonNull Map<@NonNull String, @NonNull String> customProperties) {
        this.customProperties = customProperties;
        return this;
    }

    public @NonNull CustomPhaseConfig
            withCustomPhaseCommandList(@NonNull List<@NonNull CustomPhaseCommand> customPhaseCommandList) {
        boolean hasNullCommand = Objects.requireNonNullElse(customPhaseCommandList, Collections.emptyList())
                .stream().anyMatch(Objects::isNull);
        if (hasNullCommand) {
            throw new IllegalArgumentException(
                    "Custom phase commands (" + customPhaseCommandList + ") must not contain a null element.");
        }
        this.customPhaseCommandList = customPhaseCommandList;
        return this;
    }

    public <Solution_> @NonNull CustomPhaseConfig
            withCustomPhaseCommands(@NonNull CustomPhaseCommand<Solution_> @NonNull... customPhaseCommands) {
        boolean hasNullCommand = Arrays.stream(customPhaseCommands).anyMatch(Objects::isNull);
        if (hasNullCommand) {
            throw new IllegalArgumentException(
                    "Custom phase commands (" + Arrays.toString(customPhaseCommands) + ") must not contain a null element.");
        }
        this.customPhaseCommandList = Arrays.asList(customPhaseCommands);
        return this;
    }

    @Override
    public @NonNull CustomPhaseConfig inherit(@NonNull CustomPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        customPhaseCommandClassList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandClassList, inheritedConfig.getCustomPhaseCommandClassList());
        customPhaseCommandList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandList, inheritedConfig.getCustomPhaseCommandList());
        customProperties = ConfigUtils.inheritMergeableMapProperty(
                customProperties, inheritedConfig.getCustomProperties());
        return this;
    }

    @Override
    public @NonNull CustomPhaseConfig copyConfig() {
        return new CustomPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(@NonNull Consumer<Class<?>> classVisitor) {
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
        if (customPhaseCommandClassList != null) {
            customPhaseCommandClassList.forEach(classVisitor);
        }
    }

}
