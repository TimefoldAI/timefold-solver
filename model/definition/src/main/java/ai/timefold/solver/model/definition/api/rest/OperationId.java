package ai.timefold.solver.model.definition.api.rest;

public final class OperationId {

    public static final String SOLVE_DATASET = "solveDataset";
    public static final String SCHEDULE = "schedule";
    public static final String GET_METADATA = "getMetadata";
    public static final String GET_METADATA_EVENTS = "getMetadataEvents";
    public static final String RE_SCHEDULE = "reschedule";
    public static final String FROM_INPUT = "fromInput";
    public static final String FROM_PATCH = "fromPatch";
    public static final String GET_SCHEDULES = "getSchedules";
    public static final String GET_SCHEDULE = "getSchedule";
    public static final String GET_MODEL_REQUEST = "getModelRequest";
    public static final String GET_SCHEDULE_INPUT = "getScheduleInput";
    public static final String GET_SCHEDULE_STATUS = "getScheduleStatus";
    public static final String GET_SCHEDULE_CONFIG = "getScheduleConfig";
    public static final String TERMINATE_SCHEDULE = "terminateSchedule";
    public static final String GET_SCHEDULE_LOGS = "getScheduleLogs";
    public static final String CALCULATE_SCORE_ANALYSIS = "calculateScoreAnalysis";
    public static final String GET_SCORE_ANALYSIS = "getScoreAnalysis";
    public static final String GET_VALIDATION_RESULT = "getValidationResult";
    public static final String GET_VALIDATION_ISSUE_TYPES = "getValidationIssueTypes";
    public static final String GET_VALIDATION_ISSUE_TYPE_BY_CODE = "getValidationIssueTypeByCode";

    public static final String POST_OPERATIONS_ID_PATTERN =
            "postDataset|schedule|.*recommend.*|calculateScoreAnalysis|reschedule|fromInput|fromPatch";

}
