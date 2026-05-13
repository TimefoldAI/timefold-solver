package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.io.jaxb.JaxbCustomPropertiesAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "inheritanceRate",
        "customPhaseCommandClassList",
        "customProperties",
        "constructionHeuristic"
})
@NullMarked
public class EvolutionaryIndividualGeneratorConfig extends PhaseConfig<EvolutionaryIndividualGeneratorConfig> {

    @Nullable
    private Double inheritanceRate = null;

    @XmlElement(name = "customPhaseCommandClass")
    @Nullable
    private List<Class<? extends PhaseCommand>> customPhaseCommandClassList = null;

    @XmlJavaTypeAdapter(JaxbCustomPropertiesAdapter.class)
    @Nullable
    private Map<String, String> customProperties = null;

    @Nullable
    private ConstructionHeuristicPhaseConfig constructionHeuristic = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Double getInheritanceRate() {
        return inheritanceRate;
    }

    public void setInheritanceRate(@Nullable Double inheritanceRate) {
        this.inheritanceRate = inheritanceRate;
    }

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

    public @Nullable ConstructionHeuristicPhaseConfig getConstructionHeuristic() {
        return constructionHeuristic;
    }

    public void setConstructionHeuristic(@Nullable ConstructionHeuristicPhaseConfig constructionHeuristic) {
        this.constructionHeuristic = constructionHeuristic;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryIndividualGeneratorConfig withInheritanceRate(@Nullable Double inheritanceRate) {
        setInheritanceRate(inheritanceRate);
        return this;
    }

    public EvolutionaryIndividualGeneratorConfig withCustomPhaseCommandClassList(
            List<Class<? extends PhaseCommand>> customPhaseCommandClassList) {
        setCustomPhaseCommandClassList(customPhaseCommandClassList);
        return this;
    }

    public EvolutionaryIndividualGeneratorConfig withCustomProperties(Map<String, String> customProperties) {
        setCustomProperties(customProperties);
        return this;
    }

    public EvolutionaryIndividualGeneratorConfig
            withConstructionHeuristic(@Nullable ConstructionHeuristicPhaseConfig constructionHeuristic) {
        setConstructionHeuristic(constructionHeuristic);
        return this;
    }

    @Override
    public EvolutionaryIndividualGeneratorConfig inherit(EvolutionaryIndividualGeneratorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        inheritanceRate = ConfigUtils.inheritOverwritableProperty(inheritanceRate, inheritedConfig.getInheritanceRate());
        customPhaseCommandClassList = ConfigUtils.inheritMergeableListProperty(customPhaseCommandClassList,
                inheritedConfig.getCustomPhaseCommandClassList());
        customProperties = ConfigUtils.inheritMergeableMapProperty(customProperties, inheritedConfig.getCustomProperties());
        constructionHeuristic = ConfigUtils.inheritConfig(constructionHeuristic, inheritedConfig.getConstructionHeuristic());
        return this;
    }

    @Override
    public EvolutionaryIndividualGeneratorConfig copyConfig() {
        return new EvolutionaryIndividualGeneratorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (customPhaseCommandClassList != null) {
            customPhaseCommandClassList.forEach(classVisitor);
        }
        if (constructionHeuristic != null) {
            constructionHeuristic.visitReferencedClasses(classVisitor);
        }
    }
}
