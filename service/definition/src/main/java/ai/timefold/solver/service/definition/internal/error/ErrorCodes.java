package ai.timefold.solver.service.definition.internal.error;

public class ErrorCodes {

    private static final String PREFIX = "TIMEFOLD-";

    // test data related error codes start with 0
    public static final String TEST_DATA_NOT_FOUND = PREFIX + "00001";

    // storage related error codes - starts with 1....
    public static final String STORAGE_NO_JOB_FOUND = PREFIX + "10001";
    public static final String STORAGE_UNABLE_TO_READ = PREFIX + "10002";
    public static final String STORAGE_UNABLE_TO_WRITE = PREFIX + "10003";
    public static final String STORAGE_UNKNOWN = PREFIX + "10099";

    // invalid data related error codes - starts with 2...
    public static final String INVALID_DATA = PREFIX + "20001";
    public static final String INVALID_JSON_PAYLOAD = PREFIX + "20002";
    public static final String NOT_FOUND = PREFIX + "20003";

    // extension related error codes - starts with 3...
    public static final String EXTENSION_RECOMMENDATIONS_OBJECT_TO_FIT_NOT_FOUND = PREFIX + "30100";
    public static final String EXTENSION_MODEL_VALIDATOR_FAILED = PREFIX + "30200";

    // Map service related error codes - starts with 6...
    public static final String MAP_SERVICE_PROVIDER_ERROR = PREFIX + "60001";
    public static final String MAP_SERVICE_CLIENT_CONVERT_DISTANCE_RESPONSE_ERROR = PREFIX + "60014";
    public static final String MAP_SERVICE_CLIENT_REQUEST_ERROR = PREFIX + "60015";
    public static final String MAP_SERVICE_LOCATION_NOT_IN_MAP_ERROR = PREFIX + "60017";
    public static final String MAP_SERVICE_UNKNOWN = PREFIX + "60099";

    // Solver related error codes - starts with 7...
    public static final String SOLVER_TOO_MUCH_TIME_TERMINATING_ERROR = PREFIX + "70004";
    public static final String SOLVER_UNKNOWN = PREFIX + "70005";

    // invalid configuration related error codes - starts with 8...
    public static final String INVALID_TERMINATION_CONFIG = PREFIX + "80002";

    // lastly unknown type of error
    public static final String UNKNOWN = PREFIX + "99999";

}
