package ai.timefold.solver.service.definition.internal.descriptor;

public record ModelBuildInfo(String solverVersion, String sdkVersion, String version, String buildTime, String branch,
        String buildCommit) {
}
