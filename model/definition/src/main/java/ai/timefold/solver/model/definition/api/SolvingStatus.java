package ai.timefold.solver.model.definition.api;

/**
 * Represents the dateset lifecycle.
 * <p>
 * Use case 1: a valid dataset is submitted without requesting solving.
 * In this case, the status will be DATASET_CREATED, DATASET_VALIDATED, and then DATASET_COMPUTED.
 * <p>
 * Use case 2: a valid dataset is submitted and solving is requested.
 * In this case, the status will be DATASET_CREATED, DATASET_VALIDATED, DATASET_COMPUTED, SOLVING_STARTED, SOLVING_ACTIVE,
 * SOLVING_COMPLETED.
 * SOLVING_SCHEDULED is skipped as the solving is started immediately.
 * <p>
 * Use case 3: solving is requested for a previously submitted dataset.
 * The dataset is in the DATASET_COMPUTED status before the operation starts.
 * It moves to SOLVING_SCHEDULED, SOLVING_STARTED, SOLVING_ACTIVE, and SOLVING_COMPLETED.
 * <p>
 * Use case 4: an invalid dataset is submitted.
 * In this case, the status will be DATASET_CREATED, DATASET_INVALID.
 */
public enum SolvingStatus {
    DATASET_CREATED,
    DATASET_VALIDATED,
    DATASET_INVALID,
    DATASET_COMPUTED,
    SOLVING_SCHEDULED,
    SOLVING_STARTED,
    SOLVING_ACTIVE,
    SOLVING_COMPLETED,
    SOLVING_INCOMPLETE,
    SOLVING_FAILED
}
