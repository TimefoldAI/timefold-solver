package ai.timefold.solver.model.definition.internal.events;

public class SolverChannels {

    public static final String DATASET_CREATED = "dataset-created";

    public static final String DATASET_VALIDATED = "dataset-validated";

    public static final String DATASET_COMPUTED = "dataset-computed";

    public static final String DATASET_VALIDATE_COMPUTE = "dataset-validate-compute";

    public static final String START = "schedule-start";

    public static final String TERMINATE = "schedule-terminate";

    public static final String STARTED = "schedule-started";

    public static final String TERMINATED = "schedule-terminated";

    public static final String COMPLETED = "schedule-completed";

    public static final String FAILED = "schedule-failed";

    public static final String INIT_SOLUTION = "init-schedule-solution";

    public static final String BEST_SOLUTION = "schedule-solution";

    public static final String FINAL_BEST_SOLUTION = "schedule-final-solution";

    public static final String FAILED_SOLUTION = "schedule-failed-solution";

    public static final String DATASET_EVENTS = "dataset-events";
}
