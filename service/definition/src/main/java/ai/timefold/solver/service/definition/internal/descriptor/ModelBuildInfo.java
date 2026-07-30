package ai.timefold.solver.service.definition.internal.descriptor;

public record ModelBuildInfo(String solverVersion, @Deprecated(forRemoval = true) String sdkVersion, String version,
        String buildTime, String branch, String buildCommit) {

    public ModelBuildInfo(String solverVersion, String version, String buildTime, String branch, String buildCommit) {
        this(solverVersion, solverVersion, version, buildTime, branch, buildCommit);
    }

    public static ModelBuildInfo empty() {
        return new ModelBuildInfo(null, null, null, null, null, null);
    }
}
