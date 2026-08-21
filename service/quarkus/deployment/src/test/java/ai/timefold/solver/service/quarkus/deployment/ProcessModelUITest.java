package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ai.timefold.solver.service.definition.api.ModelDescriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression coverage for the UI resources copy fix. {@code Files.copy(sourceDir, destDir, REPLACE_EXISTING)}
 * only throws {@code DirectoryNotEmptyException} when the destination directory is non-empty — which is exactly
 * what happens on an incremental build, where a previous run already populated the {@code ui/nested} output
 * directory. Before the fix, that unconditional {@code Files.copy} call ran for directory entries too and blew up
 * with an {@code UncheckedIOException} on any rebuild of a UI resource tree containing a subdirectory. The fix adds
 * an else-branch so {@code Files.copy} only runs for non-directory entries.
 */
class ProcessModelUITest {

    @TempDir
    Path tempDir;

    private Path uiSourcePath(Path moduleRoot) {
        return moduleRoot.resolve("src").resolve("main").resolve("resources").resolve("META-INF").resolve("resources");
    }

    @Test
    void copiesNestedDirectoryOnFreshBuildWithoutThrowing() throws IOException {
        Path moduleRoot = tempDir.resolve("module");
        Path outputDirectory = moduleRoot.resolve("target");
        Path uiSourcePath = uiSourcePath(moduleRoot);

        Files.createDirectories(uiSourcePath.resolve("nested"));
        Files.writeString(uiSourcePath.resolve("index.html"), "<html></html>");
        Files.writeString(uiSourcePath.resolve("nested").resolve("app.js"), "console.log('hi');");
        Files.createDirectories(outputDirectory);

        ModelDescriptor descriptor = new ModelDescriptor();
        descriptor.setId("test-model_v1");

        assertThatCode(() -> new TimefoldModelDescriptorProcessor().processModelUI(descriptor, outputDirectory))
                .doesNotThrowAnyException();

        Path uiDestination = outputDirectory.resolve("timefold").resolve(descriptor.getId()).resolve("ui");
        assertThat(uiDestination.resolve("index.html")).exists().hasContent("<html></html>");
        assertThat(uiDestination.resolve("nested").resolve("app.js")).exists().hasContent("console.log('hi');");
        assertThat(Files.isDirectory(uiDestination.resolve("nested"))).isTrue();
    }

    @Test
    void reprocessesNestedDirectoryOnIncrementalBuildWithoutThrowing() throws IOException {
        // Simulates a second (incremental) build: the destination "ui/nested" directory from a prior run
        // already exists and is non-empty. Files.copy(sourceDir, destDir, REPLACE_EXISTING) throws
        // DirectoryNotEmptyException in that case, which is exactly the bug this fix prevents.
        Path moduleRoot = tempDir.resolve("module");
        Path outputDirectory = moduleRoot.resolve("target");
        Path uiSourcePath = uiSourcePath(moduleRoot);

        Files.createDirectories(uiSourcePath.resolve("nested"));
        Files.writeString(uiSourcePath.resolve("index.html"), "<html></html>");
        Files.writeString(uiSourcePath.resolve("nested").resolve("app.js"), "console.log('hi');");
        Files.createDirectories(outputDirectory);

        ModelDescriptor descriptor = new ModelDescriptor();
        descriptor.setId("test-model_v1");

        Path uiDestination = outputDirectory.resolve("timefold").resolve(descriptor.getId()).resolve("ui");
        Files.createDirectories(uiDestination.resolve("nested"));
        Files.writeString(uiDestination.resolve("nested").resolve("app.js"), "stale content from a previous build");

        assertThatCode(() -> new TimefoldModelDescriptorProcessor().processModelUI(descriptor, outputDirectory))
                .doesNotThrowAnyException();

        assertThat(uiDestination.resolve("index.html")).exists().hasContent("<html></html>");
        assertThat(uiDestination.resolve("nested").resolve("app.js")).exists().hasContent("console.log('hi');");
    }

    @Test
    void returnsTrueWhenUiResourcesFound() throws IOException {
        Path moduleRoot = tempDir.resolve("module");
        Path outputDirectory = moduleRoot.resolve("target");
        Path uiSourcePath = uiSourcePath(moduleRoot);

        Files.createDirectories(uiSourcePath);
        Files.writeString(uiSourcePath.resolve("index.html"), "<html></html>");
        Files.createDirectories(outputDirectory);

        ModelDescriptor descriptor = new ModelDescriptor();
        descriptor.setId("test-model_v1");

        boolean found = new TimefoldModelDescriptorProcessor().processModelUI(descriptor, outputDirectory);

        assertThat(found).isTrue();
    }

    @Test
    void returnsFalseWhenUiResourcesAbsent() throws IOException {
        Path moduleRoot = tempDir.resolve("module");
        Path outputDirectory = moduleRoot.resolve("target");
        Files.createDirectories(outputDirectory);

        ModelDescriptor descriptor = new ModelDescriptor();
        descriptor.setId("test-model_v1");

        boolean found = new TimefoldModelDescriptorProcessor().processModelUI(descriptor, outputDirectory);

        assertThat(found).isFalse();
    }
}
