package ai.timefold.solver.migration.fork;

import org.openrewrite.Recipe;
import org.openrewrite.gradle.ChangeDependencyArtifactId;
import org.openrewrite.gradle.ChangeDependencyGroupId;
import org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId;

public class TimefoldChangeDependencies extends Recipe {

    private static final String[] ARTIFACT_SUFFIXES = new String[] {
            "parent",
            "bom",
            "ide-config",
            "build-parent",
            "core-parent",
            "core-impl",
            "constraint-streams-common",
            "constraint-streams-bavet",
            "constraint-streams-drools",
            "constraint-drl",
            "core",
            "persistence",
            "persistence-common",
            "persistence-xstream",
            "persistence-jaxb",
            "persistence-jackson",
            "persistence-jpa",
            "persistence-jsonb",
            "benchmark",
            "test",
            "spring-integration",
            "spring-boot-autoconfigure",
            "spring-boot-starter",
            "quarkus-integration",
            "quarkus-parent",
            "quarkus",
            "quarkus-deployment",
            "quarkus-integration-test",
            "quarkus-reflection-integration-test",
            "quarkus-devui-integration-test",
            "quarkus-drl-integration-test",
            "quarkus-benchmark-parent",
            "quarkus-benchmark",
            "quarkus-benchmark-deployment",
            "quarkus-benchmark-integration-test",
            "quarkus-jackson-parent",
            "quarkus-jackson",
            "quarkus-jackson-deployment",
            "quarkus-jackson-integration-test",
            "quarkus-jsonb-parent",
            "quarkus-jsonb",
            "quarkus-jsonb-deployment",
            "quarkus-jsonb-integration-test",
            "migration",
            "examples",
            "docs"
    };

    @Override
    public String getDisplayName() {
        return "Migrate all Maven and Gradle groupIds and artifactIds from OptaPlanner to Timefold.";
    }

    public TimefoldChangeDependencies() {
        String oldGroupId = "org.optaplanner";
        String newGroupId = "ai.timefold.solver";
        for (String artifactSuffix : ARTIFACT_SUFFIXES) {
            String oldArtifactId = "optaplanner-" + artifactSuffix;
            String newArtifactSuffix;
            if (artifactSuffix.equals("persistence")) {
                newArtifactSuffix = "persistence-parent";
            } else if (artifactSuffix.startsWith("persistence-")) {
                if (artifactSuffix.equals("persistence-common")) {
                    newArtifactSuffix = artifactSuffix;
                } else {
                    newArtifactSuffix = artifactSuffix.substring("persistence-".length());
                }
            } else {
                newArtifactSuffix = artifactSuffix;
            }
            String newArtifactId = "timefold-solver-" + newArtifactSuffix;

            // Maven
            doNext(new ChangeManagedDependencyGroupIdAndArtifactId(oldGroupId, oldArtifactId, newGroupId, newArtifactId, null));
            doNext(new ChangeDependencyGroupIdAndArtifactId(oldGroupId, oldArtifactId, newGroupId, newArtifactId, null, null));
            // Gradle
            doNext(new ChangeDependencyArtifactId(oldGroupId, oldArtifactId, newArtifactId, null));
        }
        // TODO Do not use "*" approach. This is a workaround for https://github.com/openrewrite/rewrite/issues/2994
        doNext(new ChangeDependencyGroupId(oldGroupId, "*", newGroupId, null));
    }
}
