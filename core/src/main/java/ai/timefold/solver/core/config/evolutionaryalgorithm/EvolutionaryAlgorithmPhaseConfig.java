package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "complexProblem",
        "agentCount",
        "populationConfig",
        "evolutionaryAgentConfig",
})
@NullMarked
public class EvolutionaryAlgorithmPhaseConfig extends PhaseConfig<EvolutionaryAlgorithmPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "evolutionaryAlgorithm";

    @Nullable
    private Boolean complexProblem;

    @Nullable
    private Integer agentCount;

    @Nullable
    private EvolutionaryPopulationConfig populationConfig = null;

    @Nullable
    private EvolutionaryAgentConfig evolutionaryAgentConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Boolean getComplexProblem() {
        return complexProblem;
    }

    public void setComplexProblem(@Nullable Boolean complexProblem) {
        this.complexProblem = complexProblem;
    }

    public @Nullable Integer getAgentCount() {
        return agentCount;
    }

    public void setAgentCount(@Nullable Integer agentCount) {
        this.agentCount = agentCount;
    }

    public @Nullable EvolutionaryPopulationConfig getPopulationConfig() {
        return populationConfig;
    }

    public void setPopulationConfig(@Nullable EvolutionaryPopulationConfig populationConfig) {
        this.populationConfig = populationConfig;
    }

    public @Nullable EvolutionaryAgentConfig getEvolutionaryAgentConfig() {
        return evolutionaryAgentConfig;
    }

    public void setEvolutionaryAgentConfig(@Nullable EvolutionaryAgentConfig evolutionaryAgentConfig) {
        this.evolutionaryAgentConfig = evolutionaryAgentConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAlgorithmPhaseConfig withComplexProblem(Boolean complexProblem) {
        setComplexProblem(complexProblem);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withAgentCount(Integer agentCount) {
        setAgentCount(agentCount);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withPopulationConfig(EvolutionaryPopulationConfig populationConfig) {
        setPopulationConfig(populationConfig);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withEvolutionaryAgentConfig(EvolutionaryAgentConfig evolutionaryAgentConfig) {
        setEvolutionaryAgentConfig(evolutionaryAgentConfig);
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig inherit(EvolutionaryAlgorithmPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        complexProblem = ConfigUtils.inheritOverwritableProperty(complexProblem, inheritedConfig.getComplexProblem());
        agentCount = ConfigUtils.inheritOverwritableProperty(agentCount, inheritedConfig.getAgentCount());
        populationConfig = ConfigUtils.inheritConfig(populationConfig, inheritedConfig.getPopulationConfig());
        evolutionaryAgentConfig =
                ConfigUtils.inheritConfig(evolutionaryAgentConfig, inheritedConfig.getEvolutionaryAgentConfig());
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig copyConfig() {
        return new EvolutionaryAlgorithmPhaseConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (populationConfig != null) {
            populationConfig.visitReferencedClasses(classVisitor);
        }
        if (evolutionaryAgentConfig != null) {
            evolutionaryAgentConfig.visitReferencedClasses(classVisitor);
        }
    }
}
