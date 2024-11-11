package ai.timefold.solver.core.config.phase;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlSeeAlso({
        ConstructionHeuristicPhaseConfig.class,
        CustomPhaseConfig.class,
        ExhaustiveSearchPhaseConfig.class,
        LocalSearchPhaseConfig.class,
        NoChangePhaseConfig.class,
        PartitionedSearchPhaseConfig.class
})
@XmlType(propOrder = {
        "terminationConfig"
})
public abstract class PhaseConfig<Config_ extends PhaseConfig<Config_>> extends AbstractConfig<Config_> {

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    @XmlElement(name = "termination")
    protected TerminationConfig terminationConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public @Nullable TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    public void setTerminationConfig(@Nullable TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull Config_ withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        this.setTerminationConfig(terminationConfig);
        return (Config_) this;
    }

    @Override
    public @NonNull Config_ inherit(@NonNull Config_ inheritedConfig) {
        terminationConfig = ConfigUtils.inheritConfig(terminationConfig, inheritedConfig.getTerminationConfig());
        return (Config_) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
