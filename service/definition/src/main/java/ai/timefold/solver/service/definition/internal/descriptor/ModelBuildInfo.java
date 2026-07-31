package ai.timefold.solver.service.definition.internal.descriptor;

import io.quarkus.runtime.annotations.RecordableConstructor;

public record ModelBuildInfo(String solverVersion, @Deprecated(forRemoval = true) String sdkVersion, String version,
        String buildTime, String branch, String buildCommit) {

    @RecordableConstructor
    public ModelBuildInfo {
    }

    public ModelBuildInfo(String solverVersion, String version, String buildTime, String branch, String buildCommit) {
        this(solverVersion, solverVersion, version, buildTime, branch, buildCommit);
    }

    public static ModelBuildInfo empty() {
        return new ModelBuildInfo(null, null, null, null, null, null);
    }
}
