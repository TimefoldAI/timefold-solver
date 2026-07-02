package ai.timefold.solver.core.config.evolutionaryalgorithm;

import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@XmlType(propOrder = {
        "localSearch",
})
@NullMarked
public class EvolutionaryLocalSearchConfig extends PhaseConfig<EvolutionaryLocalSearchConfig> {

    @Nullable
    private LocalSearchPhaseConfig localSearch = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable LocalSearchPhaseConfig getLocalSearch() {
        return localSearch;
    }

    public void setLocalSearch(@Nullable LocalSearchPhaseConfig localSearch) {
        this.localSearch = localSearch;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public EvolutionaryLocalSearchConfig withLocalSearch(LocalSearchPhaseConfig localSearch) {
        setLocalSearch(localSearch);
        return this;
    }

    @Override
    public EvolutionaryLocalSearchConfig inherit(EvolutionaryLocalSearchConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        localSearch = ConfigUtils.inheritConfig(localSearch, inheritedConfig.getLocalSearch());
        return this;
    }

    @Override
    public EvolutionaryLocalSearchConfig copyConfig() {
        return new EvolutionaryLocalSearchConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<@Nullable Class<?>> classVisitor) {
        if (localSearch != null) {
            localSearch.visitReferencedClasses(classVisitor);
        }
    }
}
