package ai.timefold.solver.benchmark.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.DivertingClassLoader;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.NoChangeCustomPhaseCommand;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlannerBenchmarkFactoryTest {

    // ************************************************************************
    // Static creation methods: SolverConfig XML
    // ************************************************************************

    @Test
    void createFromSolverConfigXmlResource(@TempDir Path benchmarkTestDir) {
        var benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                "ai/timefold/solver/core/config/solver/testdataSolverConfig.xml");
        var solution = new TestdataSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();

        benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                "ai/timefold/solver/core/config/solver/testdataSolverConfig.xml", benchmarkTestDir.toFile());
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();
    }

    @Test
    void createFromSolverConfigXmlResource_classLoader(@TempDir Path benchmarkTestDir) {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                "divertThroughClassLoader/ai/timefold/solver/core/api/solver/classloaderTestdataSolverConfig.xml", classLoader);
        var solution = new TestdataSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();

        benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                "divertThroughClassLoader/ai/timefold/solver/core/api/solver/classloaderTestdataSolverConfig.xml",
                benchmarkTestDir.toFile(), classLoader);
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();
    }

    @Test
    void problemIsNotASolutionInstance() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        var benchmarkFactory = PlannerBenchmarkFactory.create(
                PlannerBenchmarkConfig.createFromSolverConfig(solverConfig));
        assertThatIllegalArgumentException().isThrownBy(
                () -> benchmarkFactory.buildPlannerBenchmark("This is not a solution instance."));
    }

    @Test
    void problemIsNull() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        var benchmarkFactory = PlannerBenchmarkFactory.create(
                PlannerBenchmarkConfig.createFromSolverConfig(solverConfig));
        var solution = new TestdataSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        assertThatIllegalArgumentException().isThrownBy(() -> benchmarkFactory.buildPlannerBenchmark(solution, null));
    }

    // ************************************************************************
    // Static creation methods: XML
    // ************************************************************************

    @Test
    void createFromXmlResource() {
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfig.xml");
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromXmlResource_classLoader() {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(
                "divertThroughClassLoader/ai/timefold/solver/benchmark/api/classloaderTestdataBenchmarkConfig.xml",
                classLoader);
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromXmlResource_nonExisting() {
        final var nonExistingBenchmarkConfigResource = "ai/timefold/solver/benchmark/api/nonExistingBenchmarkConfig.xml";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlannerBenchmarkFactory.createFromXmlResource(nonExistingBenchmarkConfigResource))
                .withMessageContaining(nonExistingBenchmarkConfigResource);
    }

    @Test
    void createFromInvalidXmlResource_failsShowingBothResourceAndReason() {
        final var invalidXmlBenchmarkConfigResource = "ai/timefold/solver/benchmark/api/invalidBenchmarkConfig.xml";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlannerBenchmarkFactory.createFromXmlResource(invalidXmlBenchmarkConfigResource))
                .withMessageContaining(invalidXmlBenchmarkConfigResource)
                .withStackTraceContaining("invalidElementThatShouldNotBeHere");
    }

    @Test
    void createFromInvalidXmlFile_failsShowingBothPathAndReason(@TempDir Path benchmarkTestDir) throws IOException {
        var invalidXmlBenchmarkConfigResource = "ai/timefold/solver/benchmark/api/invalidBenchmarkConfig.xml";
        var path = benchmarkTestDir.resolve("invalidBenchmarkConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(invalidXmlBenchmarkConfigResource)) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlannerBenchmarkFactory.createFromXmlFile(path.toFile()))
                .withMessageContaining(path.toString())
                .withStackTraceContaining("invalidElementThatShouldNotBeHere");
    }

    @Test
    void createFromXmlResource_uninitializedBestSolution() {
        var benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfig.xml");
        var solverBenchmarkConfig = benchmarkConfig.getSolverBenchmarkConfigList().get(0);
        var phaseConfig = new CustomPhaseConfig();
        phaseConfig.setCustomPhaseCommandClassList(Collections.singletonList(NoChangeCustomPhaseCommand.class));
        solverBenchmarkConfig.getSolverConfig().setPhaseConfigList(Collections.singletonList(phaseConfig));
        var plannerBenchmark = PlannerBenchmarkFactory.create(benchmarkConfig).buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromXmlResource_subSingleCount() {
        var benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfig.xml");
        var solverBenchmarkConfig = benchmarkConfig.getSolverBenchmarkConfigList().get(0);
        solverBenchmarkConfig.setSubSingleCount(3);
        var plannerBenchmark = PlannerBenchmarkFactory.create(benchmarkConfig).buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromXmlFile(@TempDir Path benchmarkTestDir) throws IOException {
        var path = benchmarkTestDir.resolve("testdataBenchmarkConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfig.xml")) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromXmlFile(path.toFile());
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromXmlFile_classLoader(@TempDir Path benchmarkTestDir) throws IOException {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var path = benchmarkTestDir.resolve("classloaderTestdataBenchmarkConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/benchmark/api/classloaderTestdataBenchmarkConfig.xml")) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromXmlFile(path.toFile(), classLoader);
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    // ************************************************************************
    // Static creation methods: Freemarker
    // ************************************************************************

    @Test
    void createFromFreemarkerXmlResource() {
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromFreemarkerXmlResource(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfigTemplate.xml.ftl");
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromFreemarkerXmlResource_classLoader() {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromFreemarkerXmlResource(
                "divertThroughClassLoader/ai/timefold/solver/benchmark/api/classloaderTestdataBenchmarkConfigTemplate.xml.ftl",
                classLoader);
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromFreemarkerXmlResource_nonExisting() {
        assertThatIllegalArgumentException().isThrownBy(() -> PlannerBenchmarkFactory.createFromFreemarkerXmlResource(
                "ai/timefold/solver/benchmark/api/nonExistingBenchmarkConfigTemplate.xml.ftl"));
    }

    @Test
    void createFromFreemarkerXmlFile(@TempDir Path benchmarkTestDir) throws IOException {
        var path = benchmarkTestDir.resolve("testdataBenchmarkConfigTemplate.xml.ftl");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/benchmark/api/testdataBenchmarkConfigTemplate.xml.ftl")) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromFreemarkerXmlFile(path.toFile());
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    @Test
    void createFromFreemarkerXmlFile_classLoader(@TempDir Path benchmarkTestDir) throws IOException {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var path = benchmarkTestDir.resolve("classloaderTestdataBenchmarkConfigTemplate.xml.ftl");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/benchmark/api/classloaderTestdataBenchmarkConfigTemplate.xml.ftl")) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
        var plannerBenchmarkFactory = PlannerBenchmarkFactory.createFromFreemarkerXmlFile(path.toFile(), classLoader);
        var plannerBenchmark = plannerBenchmarkFactory.buildPlannerBenchmark();
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.benchmark()).exists();
    }

    // ************************************************************************
    // Static creation methods: PlannerBenchmarkConfig and SolverConfig
    // ************************************************************************

    @Test
    void createFromSolverConfig(@TempDir Path benchmarkTestDir) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);

        var solution = new TestdataSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));

        var benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfig(solverConfig);
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();

        benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfig(solverConfig, benchmarkTestDir.toFile());
        assertThat(benchmarkFactory.buildPlannerBenchmark(solution)).isNotNull();
    }

    // ************************************************************************
    // Instance methods
    // ************************************************************************

    @Test
    void buildPlannerBenchmark() {
        var benchmarkConfig = new PlannerBenchmarkConfig();
        var inheritedSolverConfig = new SolverBenchmarkConfig();
        inheritedSolverConfig.setSolverConfig(new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class));
        benchmarkConfig.setInheritedSolverBenchmarkConfig(inheritedSolverConfig);

        benchmarkConfig.setSolverBenchmarkConfigList(Arrays.asList(
                new SolverBenchmarkConfig(), new SolverBenchmarkConfig(), new SolverBenchmarkConfig()));

        var benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);

        var solution1 = new TestdataSolution("s1");
        solution1.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution1.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        var solution2 = new TestdataSolution("s2");
        solution2.setEntityList(Arrays.asList(new TestdataEntity("e11"), new TestdataEntity("e12"), new TestdataEntity("e13")));
        solution2.setValueList(Arrays.asList(new TestdataValue("v11"), new TestdataValue("v12")));

        var plannerBenchmark =
                (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(solution1, solution2);
        assertThat(plannerBenchmark).isNotNull();
        assertThat(plannerBenchmark.getPlannerBenchmarkResult().getSolverBenchmarkResultList()).hasSize(3);
        assertThat(plannerBenchmark.getPlannerBenchmarkResult().getUnifiedProblemBenchmarkResultList()).hasSize(2);
    }

    public static class TestdataConstraintProvider implements ConstraintProvider {
        @Override
        public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
            return new Constraint[0];
        }
    }

}
