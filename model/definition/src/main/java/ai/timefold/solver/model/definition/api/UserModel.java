package ai.timefold.solver.model.definition.api;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.model.definition.api.termination.SolverTerminationConfig;

public interface UserModel<Score_ extends Score<Score_>, Status_ extends Status<Score_>> {

    Score_ getScore();

    Status_ getStatus();

    void setStatus(Status_ status);

    Status_ newStatus(String name);

    // TODO remove this from user model once we do request/response payload split
    default SolverTerminationConfig getTermination() { // TODO remove this from user model once we do request/response payload split
        return null;
    }

    // TODO remove this from user model once we do request/response payload split
    default void setTermination(SolverTerminationConfig termination) {
        // no-op
    }
}
