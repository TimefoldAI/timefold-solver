package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "populationSize",
        "generationSize",
        "eliteSolutionSize",
        "populationRestartCount",
})
@NullMarked
public class EvolutionaryPopulationConfig extends PhaseConfig<EvolutionaryPopulationConfig> {

    @Nullable
    private Integer populationSize = null;

    @Nullable
    private Integer generationSize = null;

    @Nullable
    private Integer eliteSolutionSize = null;

    @Nullable
    private Integer populationRestartCount = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable Integer getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(@Nullable Integer populationSize) {
        this.populationSize = populationSize;
    }

    public @Nullable Integer getGenerationSize() {
        return generationSize;
    }

    public void setGenerationSize(@Nullable Integer generationSize) {
        this.generationSize = generationSize;
    }

    public @Nullable Integer getEliteSolutionSize() {
        return eliteSolutionSize;
    }

    public void setEliteSolutionSize(@Nullable Integer eliteSolutionSize) {
        this.eliteSolutionSize = eliteSolutionSize;
    }

    public @Nullable Integer getPopulationRestartCount() {
        return populationRestartCount;
    }

    public void setPopulationRestartCount(@Nullable Integer populationRestartCount) {
        this.populationRestartCount = populationRestartCount;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryPopulationConfig withPopulationSize(Integer populationSize) {
        setPopulationSize(populationSize);
        return this;
    }

    public EvolutionaryPopulationConfig withGenerationSize(Integer generationSize) {
        setGenerationSize(generationSize);
        return this;
    }

    public EvolutionaryPopulationConfig withEliteSolutionSize(Integer eliteSolutionSize) {
        setEliteSolutionSize(eliteSolutionSize);
        return this;
    }

    public EvolutionaryPopulationConfig withPopulationRestartCount(Integer populationRestartCount) {
        setPopulationRestartCount(populationRestartCount);
        return this;
    }

    @Override
    public EvolutionaryPopulationConfig inherit(EvolutionaryPopulationConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        populationSize = ConfigUtils.inheritOverwritableProperty(populationSize, inheritedConfig.getPopulationSize());
        generationSize = ConfigUtils.inheritOverwritableProperty(generationSize, inheritedConfig.getGenerationSize());
        eliteSolutionSize = ConfigUtils.inheritOverwritableProperty(eliteSolutionSize, inheritedConfig.getEliteSolutionSize());
        populationRestartCount =
                ConfigUtils.inheritOverwritableProperty(populationRestartCount, inheritedConfig.getPopulationRestartCount());
        return this;
    }

    @Override
    public EvolutionaryPopulationConfig copyConfig() {
        return new EvolutionaryPopulationConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        // Do nothing
    }
}
