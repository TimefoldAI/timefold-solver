package ai.timefold.solver.benchmark.impl.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkAggregatorTest {

    private static final BenchmarkAggregator benchmarkAggregator = new BenchmarkAggregator();

    @TempDir
    private static Path tempDir;

    private static File benchmarkDirectory;
    private static List<String> generatedBenchmarkDirectories;
    private static File testDataBenchmarkConfigXml;

    @BeforeAll
    static void setupBenchmarks() throws IOException, InterruptedException {
        // Create benchmark directory
        benchmarkDirectory = tempDir.resolve("benchmarks").toFile();
        benchmarkDirectory.mkdirs();

        // Create input directory and solution file for XML config
        File inputDir = tempDir.resolve("input").toFile();
        inputDir.mkdirs();
        File inputSolutionFile = new File(inputDir, "testSolution.xml");
        Files.writeString(inputSolutionFile.toPath(), "<TestdataSolution/>");

        // Create the benchmark config XML
        testDataBenchmarkConfigXml = tempDir.resolve("testdataBenchmarkConfig.xml").toFile();
        String xmlContent =
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <plannerBenchmark>
                            <benchmarkDirectory>%s</benchmarkDirectory>
                            <warmUpSecondsSpentLimit>0</warmUpSecondsSpentLimit>
                            <benchmarkReport>
                                <locale>en_US</locale>
                            </benchmarkReport>
                            <solverBenchmark>
                                <problemBenchmarks>
                                    <solutionFileIOClass>ai.timefold.solver.persistence.common.api.domain.solution.RigidTestdataSolutionFileIO</solutionFileIOClass>
                                    <inputSolutionFile>%s</inputSolutionFile>
                                </problemBenchmarks>
                                <solver>
                                    <solutionClass>ai.timefold.solver.core.testdomain.TestdataSolution</solutionClass>
                                    <entityClass>ai.timefold.solver.core.testdomain.TestdataEntity</entityClass>
                                    <scoreDirectorFactory>
                                        <constraintProviderClass>ai.timefold.solver.core.testdomain.TestdataConstraintProvider</constraintProviderClass>
                                    </scoreDirectorFactory>
                                    <termination>
                                        <millisecondsSpentLimit>10</millisecondsSpentLimit>
                                    </termination>
                                </solver>
                            </solverBenchmark>
                        </plannerBenchmark>
                        """
                        .formatted(benchmarkDirectory.getAbsolutePath(), inputSolutionFile.getAbsolutePath());

        Files.writeString(testDataBenchmarkConfigXml.toPath(), xmlContent);

        // Run 3 benchmarks to generate test data
        runMultipleBenchmarks(3);

        // Get the list of generated benchmark directories
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);
        generatedBenchmarkDirectories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        // Verify we have the expected number of benchmarks
        assertThat(generatedBenchmarkDirectories).hasSize(3);
    }

    @Test
    void aggregateBenchmarks_nonExistentDirectory() throws IllegalArgumentException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        File nonExistentDir = new File(tempDir.toFile(), "non_existent_directory");
        config.setBenchmarkDirectory(nonExistentDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateBenchmarks_directoryIsFile() throws IOException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        File regularFile = Files.createFile(tempDir.resolve("regularFile.txt")).toFile();
        config.setBenchmarkDirectory(regularFile);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateBenchmarks_emptyDirectory() throws IllegalArgumentException {
        File emptyDir = tempDir.resolve("empty").toFile();
        emptyDir.mkdirs();

        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(emptyDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateBenchmarks(config))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateBenchmarks_withValidResults() {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);

        File htmlReportFile = benchmarkAggregator.aggregateBenchmarks(config);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
        assertThat(htmlReportFile.getParentFile().getName()).endsWith("aggregation");
    }

    @Test
    void aggregateBenchmarks_withXmlConfig() {
        PlannerBenchmarkConfig config = PlannerBenchmarkConfig.createFromXmlFile(testDataBenchmarkConfigXml);

        File htmlReportFile = benchmarkAggregator.aggregateBenchmarks(config);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
    }

    @Test
    void aggregateSelectedBenchmarks_nonExistentDirectory() throws IllegalArgumentException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        File nonExistentDir = new File(tempDir.toFile(), "non_existent_directory");
        config.setBenchmarkDirectory(nonExistentDir);
        List<String> selectedDirectories = Arrays.asList("benchmark1", "benchmark2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateSelectedBenchmarks_directoryIsFile() throws IOException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        File regularFile = Files.createFile(tempDir.resolve("regularFile2.txt")).toFile();
        config.setBenchmarkDirectory(regularFile);
        List<String> selectedDirectories = Arrays.asList("benchmark1", "benchmark2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateSelectedBenchmarks_noValidDirectories() throws IllegalArgumentException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);
        List<String> nonExistentDirectories = Arrays.asList("nonExistent1", "nonExistent2");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateSelectedBenchmarks(config, nonExistentDirectories))
                .withMessageContaining("No valid benchmark results found in the selected directories");
    }

    @Test
    void aggregateSelectedBenchmarks_withValidDirectories() {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);

        // Use the first 2 directories from the generated benchmarks
        List<String> selectedDirectories = generatedBenchmarkDirectories.subList(0, 2);

        File htmlReportFile = benchmarkAggregator.aggregateSelectedBenchmarks(config, selectedDirectories);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
    }

    @Test
    void getAvailableBenchmarkDirectories_nonExistentDirectory() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(new File(tempDir.toFile(), "non_existent_directory"));

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories).isEmpty();
    }

    @Test
    void getAvailableBenchmarkDirectories_emptyDirectory() {
        File emptyDir = tempDir.resolve("empty2").toFile();
        emptyDir.mkdirs();

        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(emptyDir);

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories).isEmpty();
    }

    @Test
    void getAvailableBenchmarkDirectories_withValidDirectories() {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);

        List<String> directories = benchmarkAggregator.getAvailableBenchmarkDirectories(config);

        assertThat(directories).hasSize(3);
        assertThat(directories).isEqualTo(generatedBenchmarkDirectories);

        // Check that all directories have valid timestamp format
        directories.forEach(dir -> assertThat(dir).matches("\\d{4}-\\d{2}-\\d{2}_\\d{6}(_\\d+)?"));
    }

    @Test
    void aggregateAndShowReportInBrowser_nonExistentDirectory() throws IllegalArgumentException {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        File nonExistentDir = new File(tempDir.toFile(), "non_existent_directory");
        config.setBenchmarkDirectory(nonExistentDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateAndShowReportInBrowser(config))
                .withMessageContaining("Benchmark directory does not exist");
    }

    @Test
    void aggregateAndShowReportInBrowser_emptyDirectory() {
        File emptyDir = tempDir.resolve("empty3").toFile();
        emptyDir.mkdirs();

        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(emptyDir);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> benchmarkAggregator.aggregateAndShowReportInBrowser(config))
                .withMessageContaining("No benchmark results found in directory");
    }

    @Test
    void aggregateAndShowReportInBrowser_withValidResults() {
        PlannerBenchmarkConfig config = createValidBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);

        File htmlReportFile = benchmarkAggregator.aggregateAndShowReportInBrowser(config);

        assertThat(htmlReportFile).isNotNull();
        assertThat(htmlReportFile.exists()).isTrue();
        assertThat(htmlReportFile.getName()).endsWith(".html");
        // Note: Browser opening might fail in test environment, which is expected
    }

    // Helper methods

    private static PlannerBenchmarkConfig createValidBenchmarkConfig() {
        PlannerBenchmarkConfig config = new PlannerBenchmarkConfig();
        config.setBenchmarkDirectory(benchmarkDirectory);
        config.setBenchmarkReportConfig(new BenchmarkReportConfig());
        config.setWarmUpMillisecondsSpentLimit(0L);

        // Create a solver benchmark config
        SolverBenchmarkConfig solverBenchmarkConfig = new SolverBenchmarkConfig();
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withEasyScoreCalculatorClass(TestdataEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withMillisecondsSpentLimit(10L));

        solverBenchmarkConfig.setSolverConfig(solverConfig);
        config.setSolverBenchmarkConfigList(List.of(solverBenchmarkConfig));

        return config;
    }

    private static void runMultipleBenchmarks(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            runSingleBenchmark(i);
            // Small delay to ensure different timestamps
            Thread.sleep(50);
        }
    }

    private static void runSingleBenchmark(int index) {
        PlannerBenchmarkConfig benchmarkConfig = new PlannerBenchmarkConfig();
        benchmarkConfig.setBenchmarkDirectory(benchmarkDirectory);
        benchmarkConfig.setWarmUpMillisecondsSpentLimit(0L); // No warmup for test speed

        SolverBenchmarkConfig solverBenchmarkConfig = new SolverBenchmarkConfig();
        solverBenchmarkConfig.setName("TestSolver-" + index);
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withMillisecondsSpentLimit(10L));

        solverBenchmarkConfig.setSolverConfig(solverConfig);
        benchmarkConfig.setInheritedSolverBenchmarkConfig(solverBenchmarkConfig);
        benchmarkConfig.setSolverBenchmarkConfigList(List.of(new SolverBenchmarkConfig()));

        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);

        // Create a simple test problem
        TestdataSolution solution = new TestdataSolution("s" + index);
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1"),
                new TestdataEntity("e2"),
                new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(
                new TestdataValue("v1"),
                new TestdataValue("v2")));

        DefaultPlannerBenchmark plannerBenchmark =
                (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(solution);
        plannerBenchmark.benchmark(); // Run the benchmark
    }
}