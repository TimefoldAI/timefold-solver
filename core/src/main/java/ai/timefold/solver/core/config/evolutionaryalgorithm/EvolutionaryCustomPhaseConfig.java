package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "customPhaseCommandClassList",
        "customProperties",
})
@NullMarked
public class EvolutionaryCustomPhaseConfig extends PhaseConfig<EvolutionaryCustomPhaseConfig> {

    @XmlElement(name = "customPhaseCommandClass")
    @Nullable
    private List<Class<? extends PhaseCommand>> customPhaseCommandClassList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    @Nullable
    private Map<String, String> customProperties = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable List<Class<? extends PhaseCommand>> getCustomPhaseCommandClassList() {
        return customPhaseCommandClassList;
    }

    public void setCustomPhaseCommandClassList(
            @Nullable List<Class<? extends PhaseCommand>> customPhaseCommandClassList) {
        this.customPhaseCommandClassList = customPhaseCommandClassList;
    }

    public @Nullable Map<String, String> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(@Nullable Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryCustomPhaseConfig withCustomPhaseCommandClassList(
            List<Class<? extends PhaseCommand>> customPhaseCommandClassList) {
        setCustomPhaseCommandClassList(customPhaseCommandClassList);
        return this;
    }

    public EvolutionaryCustomPhaseConfig withCustomProperties(Map<String, String> customProperties) {
        setCustomProperties(customProperties);
        return this;
    }

    @Override
    public EvolutionaryCustomPhaseConfig inherit(EvolutionaryCustomPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        customPhaseCommandClassList = ConfigUtils.inheritMergeableListProperty(customPhaseCommandClassList,
                inheritedConfig.getCustomPhaseCommandClassList());
        customProperties = ConfigUtils.inheritMergeableMapProperty(customProperties, inheritedConfig.getCustomProperties());
        return this;
    }

    @Override
    public EvolutionaryCustomPhaseConfig copyConfig() {
        return new EvolutionaryCustomPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (customPhaseCommandClassList != null) {
            customPhaseCommandClassList.forEach(classVisitor);
        }
    }
}
