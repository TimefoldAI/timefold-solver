package ai.timefold.solver.service.definition.api.executionprofile;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The always-available baseline {@link ExecutionProfile}, applied when a run selects no other profile. It contributes no
 * runtime specification of its own.
 */
@ApplicationScoped
public class DefaultExecutionProfile implements ExecutionProfile {

    /**
     * Identifier of the always-available baseline profile, applied when a run selects no other profile.
     */
    public static final String NAME = "default";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Standard runtime configuration used for regular runs";
    }
}
