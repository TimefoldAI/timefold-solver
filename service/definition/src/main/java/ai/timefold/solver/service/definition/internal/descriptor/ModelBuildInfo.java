package ai.timefold.solver.service.definition.internal.descriptor;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * Holds build-time metadata about the Timefold Solver model, including version information and source control details
 * captured at build time.
 * 
 * @param solverVersion the version of the Timefold Solver used to build the model
 * @param sdkVersion the version of the SDK; deprecated, use {@link #solverVersion()} instead
 * @param version the version of the model
 * @param buildTime the timestamp when the model was built
 * @param branch the source control branch from which the model was built
 * @param buildCommit the source control commit hash from which the model was built
 */
public record ModelBuildInfo(String solverVersion, @Deprecated(forRemoval = true) String sdkVersion, String version,
        String buildTime, String branch, String buildCommit) {

    private static final ModelBuildInfo EMPTY = new ModelBuildInfo(null,null, null, null, null);

    @RecordableConstructor
    public ModelBuildInfo {
    }

    public ModelBuildInfo(String solverVersion, String version, String buildTime, String branch, String buildCommit) {
        this(solverVersion, solverVersion, version, buildTime, branch, buildCommit);
    }

    public static ModelBuildInfo empty() {
        return EMPTY;
    }
}
