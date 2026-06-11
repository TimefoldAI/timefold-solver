package ai.timefold.solver.service.definition.internal.platform;

public class EnvironmentVars {

    /**
     * Identifier of the job to be used by runtime, data set stored with given identifier
     */
    public static final String ENV_TIMEFOLD_JOB_ID = "AI_TIMEFOLD_JOB_ID";

    /**
     * duration (e.g. P2D) to keep the runtime instance active despite solving might have been already completed
     */
    public static final String ENV_TIMEFOLD_RUNTIME_TTL = "AI_TIMEFOLD_RUNTIME_TTL";

    /**
     * duration (e.g. P2D) to keep the runtime instance active despite being idle to be able to process incoming requests
     */
    public static final String ENV_TIMEFOLD_IDLE_RUNTIME_TTL = "AI_TIMEFOLD_IDLE_RUNTIME_TTL";

    /**
     * Provides hint to solver worker what operation is expected upon start
     */
    public static final String ENV_TIMEFOLD_ON_START_COMMAND = "AI_TIMEFOLD_ON_START_COMMAND";

    /**
     * Plan name that is being used to run the job
     */
    public static final String ENV_TIMEFOLD_PLAN_NAME = "AI_TIMEFOLD_PLAN_NAME";

    /**
     * Tenant name
     */
    public static final String ENV_TIMEFOLD_TENANT_NAME = "AI_TIMEFOLD_TENANT_NAME";

    /**
     * Configured map-service location: either a concrete region (e.g. {@code us-georgia}) or the
     * sentinel {@link #MAP_SERVICE_LOCATION_AUTO_SELECT} for runtime region resolution.
     */
    public static final String ENV_TIMEFOLD_PLATFORM_MAP_SERVICE_LOCATION = "AI_TIMEFOLD_PLATFORM_MAP_SERVICE_LOCATION";

    /**
     * Sentinel value for {@link #ENV_TIMEFOLD_PLATFORM_MAP_SERVICE_LOCATION}: tells the maps-service
     * to auto-select the region at request time based on the locations being routed.
     */
    public static final String MAP_SERVICE_LOCATION_AUTO_SELECT = "auto-select";

    /**
     * Kubernetes API specific environment variables that are set based on execution information like pod and node
     */
    public static final String K8S_INFO_NODE_NAME = "NODE_NAME";
    public static final String K8S_INFO_CPU_LIMIT = "CPU_LIMIT";
    public static final String K8S_INFO_MEMORY_LIMIT = "MEMORY_LIMIT";

}
