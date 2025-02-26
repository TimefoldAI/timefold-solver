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

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "customPhaseCommandClassList",
        "customProperties",
})
@NullMarked
public class CustomPhaseConfig extends PhaseConfig<CustomPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "customPhase";

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    @XmlElement(name = "customPhaseCommandClass")
    protected @Nullable List<Class<? extends PhaseCommand>> customPhaseCommandClassList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    protected @Nullable Map<String, String> customProperties = null;

    @XmlTransient
    protected @Nullable List<? extends PhaseCommand> customPhaseCommandList = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable List<Class<? extends PhaseCommand>> getCustomPhaseCommandClassList() {
        return customPhaseCommandClassList;
    }

    public void setCustomPhaseCommandClassList(@Nullable List<Class<? extends PhaseCommand>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
    }

    public @Nullable Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(@Nullable Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    public @Nullable List<? extends PhaseCommand> getCustomPhaseCommandList() {
        return customPhaseCommandList;
    }

    public void setCustomPhaseCommandList(@Nullable List<? extends PhaseCommand> customPhaseCommandList) {
        this.customPhaseCommandList = customPhaseCommandList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public CustomPhaseConfig withCustomPhaseCommandClassList(List<Class<? extends PhaseCommand>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
        return this;
    }

    public CustomPhaseConfig withCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
        return this;
    }

    public CustomPhaseConfig withCustomPhaseCommandList(List<? extends PhaseCommand> customPhaseCommandList) {
        boolean hasNullCommand = Objects.requireNonNullElse(customPhaseCommandList, Collections.emptyList())
                .stream().anyMatch(Objects::isNull);
        if (hasNullCommand) {
            throw new IllegalArgumentException(
                    "Custom phase commands (" + customPhaseCommandList + ") must not contain a null element.");
        }
        this.customPhaseCommandList = List.copyOf(customPhaseCommandList);
        return this;
    }

    public <Solution_> CustomPhaseConfig withCustomPhaseCommands(PhaseCommand<Solution_>... customPhaseCommands) {
        return withCustomPhaseCommandList(Arrays.asList(customPhaseCommands));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public CustomPhaseConfig inherit(CustomPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        customPhaseCommandClassList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandClassList, inheritedConfig.getCustomPhaseCommandClassList());
        customPhaseCommandList = ConfigUtils.inheritMergeableListProperty(
                customPhaseCommandList, (List) inheritedConfig.getCustomPhaseCommandList());
        customProperties = ConfigUtils.inheritMergeableMapProperty(
                customProperties, inheritedConfig.getCustomProperties());
        return this;
    }

    @Override
    public CustomPhaseConfig copyConfig() {
        return new CustomPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        if (terminationConfig != null) {
            terminationConfig.visitReferencedClasses(classVisitor);
        }
        if (customPhaseCommandClassList != null) {
            customPhaseCommandClassList.forEach(classVisitor);
        }
    }

}
