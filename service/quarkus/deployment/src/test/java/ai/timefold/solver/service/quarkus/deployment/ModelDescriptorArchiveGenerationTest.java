package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import ai.timefold.solver.service.quarkus.deployment.builditem.ModelArchiveBuildItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

class ModelDescriptorArchiveGenerationTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesExistingEnhancedOpenApiDefinition() throws IOException {
        Path modelArchivePath = tempDir.resolve("timefold");
        Path outputDirectory = tempDir.resolve("target");
        Path modelFilesPath = modelArchivePath.resolve("test-model_v1");

        Files.createDirectories(modelFilesPath.resolve("openapi"));
        Files.createDirectories(modelFilesPath.resolve("jsonschema"));
        Files.createDirectories(modelFilesPath.resolve("default-config-profile"));
        Files.createDirectories(outputDirectory);

        Files.writeString(modelArchivePath.resolve("timefold-model-descriptor.json"),
                """
                        {
                          "id": "test-model_v1",
                          "imageRef": "test-image:latest"
                        }
                        """);
        Files.writeString(modelFilesPath.resolve("openapi/service.json"), "{\"updated\":true}");
        Files.writeString(modelFilesPath.resolve("jsonschema/schedule.json"), "{}");
        Files.writeString(modelFilesPath.resolve("default-config-profile/default-config.json"), "{}");

        Path enhancedOpenApiPath = outputDirectory.resolve("timefold-model-enhanced-openapi.json");
        Files.writeString(enhancedOpenApiPath, "{\"stale\":true}");

        new TimefoldModelDescriptorProcessor()
                .generateModelDescriptorArchive(Optional.of(new ModelArchiveBuildItem(modelArchivePath)),
                        new OutputTargetBuildItem(outputDirectory, "test", "test", false, new Properties(),
                                Optional.empty()));

        assertThat(Files.readString(enhancedOpenApiPath)).isEqualTo("{\"updated\":true}");
        assertThat(outputDirectory.resolve("model-descriptor.zip")).exists();
    }

}
