package ai.timefold.solver.service.definition.impl.executionprofile.distancematrix;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;

/**
 * Stores the travel-time and distance matrices the solver used, so that they can be downloaded and the run reproduced
 * later without access to the maps service.
 * <p>
 * The profile switches on the maps client's recorder, which writes one JSON file per matrix into the {@value #ID}
 * subdirectory of the execution-profile artifacts directory ({@link EnvironmentVars#DEFAULT_EXECUTION_PROFILE_DIR}).
 */
public final class DistanceMatrixExecutionProfile implements ExecutionProfile {

    static final String ID = "distance-matrix";

    static final String ENV_DISTANCE_MATRIX_RECORDING_ENABLED = "TIMEFOLD_PLATFORM_MAP_SERVICE_RECORDING_ENABLED";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return "Store distance matrix";
    }

    @Override
    public String description() {
        return "Stores the travel time and distance matrices used by the solver in the run's execution artifacts, so "
                + "the run can be reproduced without the maps service.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of(ENV_DISTANCE_MATRIX_RECORDING_ENABLED, "true");
    }
}
