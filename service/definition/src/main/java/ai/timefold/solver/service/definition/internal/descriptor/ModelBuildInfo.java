package ai.timefold.solver.service.definition.internal.descriptor;

public record ModelBuildInfo(String solverVersion, String version, String buildTime, String branch,
        String buildCommit) {

    public static ModelBuildInfo empty() {
        return new ModelBuildInfo(null, null, null, null, null);
    }
}
