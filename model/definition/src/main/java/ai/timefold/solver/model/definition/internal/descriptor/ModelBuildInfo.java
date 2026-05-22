package ai.timefold.solver.model.definition.internal.descriptor;

public record ModelBuildInfo(String solverVersion, String sdkVersion, String version, String buildTime, String branch,
        String buildCommit) {
}
