package ai.timefold.solver.service.definition.api;

import java.time.OffsetDateTime;
import java.util.Map;

public interface Status<Score_> {

    void solvingStarted();

    void solvingActive();

    void updateStatusOnSave(SolvingStatus solverStatus, Score_ score);

    void updateStatusOnComplete(SolvingStatus solverStatus, Score_ score);

    void updateStatusOnFailure(String failureMessage);

    SolvingStatus getSolverStatus();

    OffsetDateTime getSubmitDateTime();

    OffsetDateTime getStartDateTime();

    OffsetDateTime getActiveDateTime();

    OffsetDateTime getCompleteDateTime();

    OffsetDateTime getShutdownDateTime();

    String getId();

    String getName();

    default String defaultJobName() {
        return getClass().getSimpleName();
    }

    Map<String, String> asMap();
}
