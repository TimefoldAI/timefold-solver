package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "complexProblem",
        "populationConfig",
        "workerConfig",
})
@NullMarked
public class EvolutionaryAlgorithmPhaseConfig extends PhaseConfig<EvolutionaryAlgorithmPhaseConfig> {

    public static final String XML_ELEMENT_NAME = "evolutionaryAlgorithm";

    @Nullable
    private Boolean complexProblem;

    @Nullable
    private EvolutionaryPopulationConfig populationConfig = null;

    @Nullable
    private EvolutionaryWorkerConfig workerConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Boolean getComplexProblem() {
        return complexProblem;
    }

    public void setComplexProblem(@Nullable Boolean complexProblem) {
        this.complexProblem = complexProblem;
    }

    public @Nullable EvolutionaryPopulationConfig getPopulationConfig() {
        return populationConfig;
    }

    public void setPopulationConfig(@Nullable EvolutionaryPopulationConfig populationConfig) {
        this.populationConfig = populationConfig;
    }

    public @Nullable EvolutionaryWorkerConfig getWorkerConfig() {
        return workerConfig;
    }

    public void setWorkerConfig(@Nullable EvolutionaryWorkerConfig workerConfig) {
        this.workerConfig = workerConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAlgorithmPhaseConfig withComplexProblem(Boolean complexProblem) {
        setComplexProblem(complexProblem);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withPopulationConfig(EvolutionaryPopulationConfig populationConfig) {
        setPopulationConfig(populationConfig);
        return this;
    }

    public EvolutionaryAlgorithmPhaseConfig withWorkerConfig(EvolutionaryWorkerConfig evolutionaryWorkerConfig) {
        setWorkerConfig(evolutionaryWorkerConfig);
        return this;
    }

    @Override
    public EvolutionaryAlgorithmPhaseConfig inherit(EvolutionaryAlgorithmPhaseConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        complexProblem = ConfigUtils.inheritOverwritableProperty(complexProblem, inheritedConfig.getComplexProblem());
        populationConfig = ConfigUtils.inheritConfig(populationConfig, inheritedConfig.getPopulationConfig());
        workerConfig =
                ConfigUtils.inheritConfig(workerConfig, inheritedConfig.getWorkerConfig());
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
        if (workerConfig != null) {
            workerConfig.visitReferencedClasses(classVisitor);
        }
    }
}
