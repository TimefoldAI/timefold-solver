package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "customIndividualPhaseConfig",
        "localSearchPhaseConfig",
})
@NullMarked
public class EvolutionaryWorkerConfig extends PhaseConfig<EvolutionaryWorkerConfig> {

    @Nullable
    private EvolutionaryCustomPhaseConfig customIndividualPhaseConfig = null;

    @Nullable
    private LocalSearchPhaseConfig localSearchPhaseConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable EvolutionaryCustomPhaseConfig getCustomIndividualPhaseConfig() {
        return customIndividualPhaseConfig;
    }

    public void setCustomIndividualPhaseConfig(@Nullable EvolutionaryCustomPhaseConfig customIndividualPhaseConfig) {
        this.customIndividualPhaseConfig = customIndividualPhaseConfig;
    }

    public @Nullable LocalSearchPhaseConfig getLocalSearchPhaseConfig() {
        return localSearchPhaseConfig;
    }

    public void setLocalSearchPhaseConfig(@Nullable LocalSearchPhaseConfig localSearchPhaseConfig) {
        this.localSearchPhaseConfig = localSearchPhaseConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryWorkerConfig
            withCustomIndividualPhaseConfig(EvolutionaryCustomPhaseConfig customIndividualPhaseConfig) {
        setCustomIndividualPhaseConfig(customIndividualPhaseConfig);
        return this;
    }

    public EvolutionaryWorkerConfig withLocalSearchPhaseConfig(LocalSearchPhaseConfig localSearchPhaseConfig) {
        setLocalSearchPhaseConfig(localSearchPhaseConfig);
        return this;
    }

    @Override
    public EvolutionaryWorkerConfig inherit(EvolutionaryWorkerConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        customIndividualPhaseConfig =
                ConfigUtils.inheritConfig(customIndividualPhaseConfig, inheritedConfig.getCustomIndividualPhaseConfig());
        localSearchPhaseConfig = ConfigUtils.inheritConfig(localSearchPhaseConfig, inheritedConfig.getLocalSearchPhaseConfig());
        return this;
    }

    @Override
    public EvolutionaryWorkerConfig copyConfig() {
        return new EvolutionaryWorkerConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (customIndividualPhaseConfig != null) {
            customIndividualPhaseConfig.visitReferencedClasses(classVisitor);
        }
        if (localSearchPhaseConfig != null) {
            localSearchPhaseConfig.visitReferencedClasses(classVisitor);
        }
    }
}
