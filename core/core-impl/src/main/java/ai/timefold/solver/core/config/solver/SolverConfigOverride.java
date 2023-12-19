package ai.timefold.solver.core.config.solver;

import java.util.function.Consumer;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

public class SolverConfigOverride extends AbstractConfig<SolverConfigOverride> {

    private TerminationConfig terminationConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    public void setTerminationConfig(TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public SolverConfigOverride withTerminationConfig(TerminationConfig terminationConfig) {
        this.setTerminationConfig(terminationConfig);
        return this;
    }

    @Override
    public SolverConfigOverride inherit(SolverConfigOverride inheritedConfig) {
        terminationConfig = ConfigUtils.inheritConfig(terminationConfig, inheritedConfig.getTerminationConfig());
        return this;
    }

    @Override
    public SolverConfigOverride copyConfig() {
        return null;
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        if (getTerminationConfig() != null) {
            getTerminationConfig().visitReferencedClasses(classVisitor);
        }
    }
}
