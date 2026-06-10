package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "individualGeneratorConfig",
        "localSearchConfig",
})
@NullMarked
public class EvolutionaryAgentConfig extends PhaseConfig<EvolutionaryAgentConfig> {

    @Nullable
    private EvolutionaryIndividualGeneratorConfig individualGeneratorConfig = null;

    @Nullable
    private EvolutionaryLocalSearchConfig localSearchConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable EvolutionaryIndividualGeneratorConfig getIndividualGeneratorConfig() {
        return individualGeneratorConfig;
    }

    public void setIndividualGeneratorConfig(@Nullable EvolutionaryIndividualGeneratorConfig individualGeneratorConfig) {
        this.individualGeneratorConfig = individualGeneratorConfig;
    }

    public @Nullable EvolutionaryLocalSearchConfig getLocalSearchConfig() {
        return localSearchConfig;
    }

    public void setLocalSearchConfig(@Nullable EvolutionaryLocalSearchConfig localSearchConfig) {
        this.localSearchConfig = localSearchConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryAgentConfig
            withIndividualGeneratorConfig(EvolutionaryIndividualGeneratorConfig individualGeneratorConfig) {
        setIndividualGeneratorConfig(individualGeneratorConfig);
        return this;
    }

    public EvolutionaryAgentConfig withLocalSearchConfig(EvolutionaryLocalSearchConfig localSearchConfig) {
        setLocalSearchConfig(localSearchConfig);
        return this;
    }

    @Override
    public EvolutionaryAgentConfig inherit(EvolutionaryAgentConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        individualGeneratorConfig =
                ConfigUtils.inheritConfig(individualGeneratorConfig, inheritedConfig.getIndividualGeneratorConfig());
        localSearchConfig = ConfigUtils.inheritConfig(localSearchConfig, inheritedConfig.getLocalSearchConfig());
        return this;
    }

    @Override
    public EvolutionaryAgentConfig copyConfig() {
        return new EvolutionaryAgentConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (individualGeneratorConfig != null) {
            individualGeneratorConfig.visitReferencedClasses(classVisitor);
        }
        if (localSearchConfig != null) {
            localSearchConfig.visitReferencedClasses(classVisitor);
        }
    }
}
