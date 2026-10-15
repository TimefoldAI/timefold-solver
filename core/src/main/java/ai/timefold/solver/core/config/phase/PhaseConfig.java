package ai.timefold.solver.core.config.phase;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@XmlSeeAlso({
        ConstructionHeuristicPhaseConfig.class,
        CustomPhaseConfig.class,
        ExhaustiveSearchPhaseConfig.class,
        LocalSearchPhaseConfig.class,
        PartitionedSearchPhaseConfig.class
})
@XmlType(propOrder = {
        "environmentMode",
        "terminationConfig"
})
public abstract class PhaseConfig<Config_ extends PhaseConfig<Config_>> extends AbstractConfig<Config_> {

    // Warning: all fields are null (and not defaulted) because they can be inherited
    // and also because the input config file should match the output config file

    // Per phase environment
    protected EnvironmentMode environmentMode = null;

    @XmlElement(name = "termination")
    protected TerminationConfig terminationConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    /**
     * @return null when this phase runs in the solver's {@link EnvironmentMode}
     * @see #setEnvironmentMode(EnvironmentMode)
     */
    public @Nullable EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    /**
     * Overrides the solver's {@link EnvironmentMode} for this phase only.
     * <p>
     * Null, the default, means the phase runs in the solver's environment mode.
     * A non-null value must obey two rules, both checked when the
     * {@link SolverFactory SolverFactory} is built,
     * so that a violation fails there rather than during solving:
     * <ul>
     * <li>it may not be less strict than the solver's environment mode;</li>
     * <li>it may not be set at all when the solver's environment mode is
     * {@link EnvironmentMode#NON_REPRODUCIBLE}.</li>
     * </ul>
     *
     * @param environmentMode null to run this phase in the solver's environment mode
     */
    public void setEnvironmentMode(@Nullable EnvironmentMode environmentMode) {
        this.environmentMode = environmentMode;
    }

    public @Nullable TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    public void setTerminationConfig(@Nullable TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public @NonNull Config_ withEnvironmentMode(@NonNull EnvironmentMode environmentMode) {
        this.setEnvironmentMode(environmentMode);
        return (Config_) this;
    }

    public @NonNull Config_ withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        this.setTerminationConfig(terminationConfig);
        return (Config_) this;
    }

    @Override
    public @NonNull Config_ inherit(@NonNull Config_ inheritedConfig) {
        environmentMode = ConfigUtils.inheritOverwritableProperty(environmentMode, inheritedConfig.getEnvironmentMode());
        terminationConfig = ConfigUtils.inheritConfig(terminationConfig, inheritedConfig.getTerminationConfig());
        return (Config_) this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
