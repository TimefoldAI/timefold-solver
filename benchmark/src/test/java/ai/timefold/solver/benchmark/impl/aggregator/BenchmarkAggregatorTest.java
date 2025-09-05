package ai.timefold.solver.benchmark.impl.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkAggregatorTest {

    private BenchmarkAggregator benchmarkAggregator;

    @BeforeEach
    void setUp() {
        benchmarkAggregator = new BenchmarkAggregator();
    }

    @Test
    void aggregateBenchmarks_nonExistentDirectory(@TempDir Path tempDir) {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig(tempDir);
        File nonExistentDir = new File("non/existent/directory");
        config.setBenchmarkDirectory(nonExistentDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateBenchmarks_directoryIsFile(@TempDir Path tempDir) throws IOException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig(tempDir);
        File regularFile = Files.createFile(tempDir.resolve("regularFile.txt")).toFile();
        config.setBenchmarkDirectory(regularFile);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateBenchmarks_emptyDirectory(@TempDir Path tempDir) {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig(tempDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateBenchmarks_withValidResults() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");

        File htmlReportFile = benchmarkAggregator.aggregateBenchmarks(config);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
    }

    @Test
    void aggregateSelectedBenchmarks_nonExistentDirectory(@TempDir Path tempDir) {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig(tempDir);
        File nonExistentDir = new File("non/existent/directory");
        config.setBenchmarkDirectory(nonExistentDir);
        List<String> selectedDirectories = Arrays.asList("benchmark1", "benchmark2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateSelectedBenchmarks_directoryIsFile(@TempDir Path tempDir) throws IOException {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");
        File regularFile = Files.createFile(tempDir.resolve("regularFile.txt")).toFile();
        config.setBenchmarkDirectory(regularFile);
        List<String> selectedDirectories = Arrays.asList("benchmark1", "benchmark2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateSelectedBenchmarks_noValidDirectories() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");
        List<String> nonExistentDirectories = Arrays.asList("nonExistent1", "nonExistent2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, nonExistentDirectories))
                .withMessageContaining("No valid benchmark results found for selected directories");
    }

    @Test
    void aggregateSelectedBenchmarks_withValidDirectories() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");
        List<String> availableDirectories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);
        List<String> selectedDirectories = availableDirectories.subList(0, 2);

        File htmlReportFile = benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
    }

    @Test
    void getAvailableBenchmarkDirectories_nonExistentDirectory() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(new File("non/existent/directory"));

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories).isEmpty();
    }

    @Test
    void getAvailableBenchmarkDirectories_emptyDirectory(@TempDir Path tempDir) {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(tempDir.toFile());

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories).isEmpty();
    }

    @Test
    void getAvailableBenchmarkDirectories_withValidDirectories() throws IOException {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories)
                .hasSize(4)
                .containsExactlyInAnyOrder("2025-09-05_141529", "2025-09-05_141529_1", "2025-09-05_141530",
                        "2025-09-05_141530_1");
    }

    @Test
    void aggregateAndShowReportInBrowser_nonExistentDirectory() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");
        File nonExistentDir = new File("non/existent/directory");
        config.setBenchmarkDirectory(nonExistentDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateAndShowReportInBrowser(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateAndShowReportInBrowser_emptyDirectory(@TempDir Path tempDir) {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig(tempDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateAndShowReportInBrowser(config))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateAndShowReportInBrowser_withValidResults() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig
                .createFromXmlResource("ai/timefold/solver/benchmark/api/testdataBenchmarkConfigWithReport.xml");

        File htmlReportFile = benchmarkAggregator.aggregateAndShowReportInBrowser(config);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
        // Note: Browser opening might fail in test environment, which is expected
    }

    @AfterAll
    static void cleanupAggregationFolders() {
        try {
            Path resourcesPath = Path.of("src/test/resources/ai/timefold/solver/benchmark/impl/result/aggregator");
            if (Files.exists(resourcesPath)) {
                try (var paths = Files.walk(resourcesPath)) {
                    paths.filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().endsWith("aggregation"))
                            .sorted(Comparator.reverseOrder()) // Delete deepest directories first
                            .forEach(path -> {
                                try {
                                    deleteDirectoryRecursively(path);
                                    System.out.println("Deleted aggregation folder: " + path);
                                } catch (IOException e) {
                                    System.err.println("Failed to delete aggregation folder: " + path + " - " + e.getMessage());
                                }
                            });
                }
            }
        } catch (IOException e) {
            System.err.println("Error during aggregation folders cleanup: " + e.getMessage());
        }
    }

    // Helper methods

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()) // Delete files first, then directories
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete: " + path, e);
                            }
                        });
            }
        }
    }

    private PlannerBenchmarkConfig createValidBenchmarkConfig(Path tempDir) {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(tempDir.toFile());
        config.setBenchmarkReportConfig(new BenchmarkReportConfig());
        config.setWarmUpMillisecondsSpentLimit(0L);

        // Create a solver benchmark config
        SolverBenchmarkConfig solverBenchmarkConfig = new SolverBenchmarkConfig();
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withSecondsSpentLimit(1L));

        solverBenchmarkConfig.setSolverConfig(solverConfig);
        config.setSolverBenchmarkConfigList(List.of(solverBenchmarkConfig));

        return config;
    }

}