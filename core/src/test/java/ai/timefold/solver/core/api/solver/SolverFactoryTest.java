package ai.timefold.solver.core.api.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.phase.PossiblyInitializingPhase;
import ai.timefold.solver.core.impl.score.DummySimpleScoreEasyScoreCalculator;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.NoChangeCustomPhaseCommand;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SolverFactoryTest {

    private static File solverTestDir;

    @BeforeAll
    static void setup() {
        solverTestDir = new File("target/test/solverTest/");
        solverTestDir.mkdirs();
    }

    @Test
    void createFromXmlResource() {
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.createFromXmlResource(
                "ai/timefold/solver/core/api/solver/testdataSolverConfig.xml");
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void createFromXmlResource_noGenericsForBackwardsCompatibility() {
        SolverFactory solverFactory = SolverFactory.createFromXmlResource(
                "ai/timefold/solver/core/api/solver/testdataSolverConfig.xml");
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    void createFromNonExistingXmlResource_failsShowingResource() {
        final var xmlSolverConfigResource = "ai/timefold/solver/core/api/solver/nonExistingSolverConfig.xml";
        assertThatIllegalArgumentException().isThrownBy(() -> SolverFactory.createFromXmlResource(xmlSolverConfigResource))
                .withMessageContaining(xmlSolverConfigResource);
    }

    @Test
    void createFromNonExistingXmlFile_failsShowingPath() {
        final var xmlSolverConfigFile = new File(solverTestDir, "nonExistingSolverConfig.xml");
        assertThatIllegalArgumentException().isThrownBy(() -> SolverFactory.createFromXmlFile(xmlSolverConfigFile))
                .withMessageContaining(xmlSolverConfigFile.toString());
    }

    @Test
    void createFromXmlResource_classLoader() {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.createFromXmlResource(
                "divertThroughClassLoader/ai/timefold/solver/core/api/solver/classloaderTestdataSolverConfig.xml", classLoader);
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    void createFromXmlFile() throws IOException {
        var file = new File(solverTestDir, "testdataSolverConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/core/api/solver/testdataSolverConfig.xml")) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.createFromXmlFile(file);
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    void createFromXmlFile_classLoader() throws IOException {
        // Mocking loadClass doesn't work well enough, because the className still differs from class.getName()
        ClassLoader classLoader = new DivertingClassLoader(getClass().getClassLoader());
        var file = new File(solverTestDir, "classloaderTestdataSolverConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(
                "ai/timefold/solver/core/api/solver/classloaderTestdataSolverConfig.xml")) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.createFromXmlFile(file, classLoader);
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    void createFromInvalidXmlResource_failsShowingBothResourceAndReason() {
        final var invalidXmlSolverConfigResource = "ai/timefold/solver/core/api/solver/invalidSolverConfig.xml";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SolverFactory.createFromXmlResource(invalidXmlSolverConfigResource))
                .withMessageContaining(invalidXmlSolverConfigResource)
                .withStackTraceContaining("invalidElementThatShouldNotBeHere");
    }

    @Test
    void createFromInvalidXmlFile_failsShowingBothPathAndReason() throws IOException {
        final var invalidXmlSolverConfigResource = "ai/timefold/solver/core/api/solver/invalidSolverConfig.xml";
        var file = new File(solverTestDir, "invalidSolverConfig.xml");
        try (var in = getClass().getClassLoader().getResourceAsStream(invalidXmlSolverConfigResource)) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SolverFactory.createFromXmlFile(file))
                .withMessageContaining(file.toString())
                .withStackTraceContaining("invalidElementThatShouldNotBeHere");
    }

    @Test
    void create() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();
        assertThat(solver).isNotNull();
    }

    @Test
    void createAndOverrideSettings() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        SolverConfigOverride<TestdataSolution> configOverride = mock(SolverConfigOverride.class);
        var terminationConfig = new TerminationConfig();
        terminationConfig.withSpentLimit(Duration.ofSeconds(60));
        doReturn(terminationConfig).when(configOverride).getTerminationConfig();
        var solver = solverFactory.buildSolver(configOverride);
        assertThat(solver).isNotNull();
        verify(configOverride, atLeast(1)).getTerminationConfig();
    }

    @Test
    void getScoreDirectorFactory() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var solverFactory = (DefaultSolverFactory<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig);
        var scoreDirectorFactory = solverFactory.getScoreDirectorFactory();
        assertThat(scoreDirectorFactory).isNotNull();

        var solution = new TestdataSolution("s1");
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        try (var scoreDirector = scoreDirectorFactory.buildScoreDirector()) {
            scoreDirector.setWorkingSolution(solution);
            var score = scoreDirector.calculateScore();
            assertThat(score).isNotNull();
        }
    }

    @Test
    void localSearchAfterUnterminatedLocalSearch() {
        // Create a solver config that has two local searches, the second one unreachable.
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig(),
                        new LocalSearchPhaseConfig());

        var solverFactory =
                (DefaultSolverFactory<TestdataSolution>) SolverFactory.<TestdataSolution> create(solverConfig);
        Assertions.assertThatThrownBy(solverFactory::buildSolver)
                .hasMessageContaining("unreachable phase");
    }

    @Test
    void validateInitializationPhases() {
        // Default configuration
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        var firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isTrue();
        assertThat(solver.getPhaseList().get(1))
                .isNotInstanceOf(PossiblyInitializingPhase.class);

        // Only CH
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isFalse();

        // Only CH
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new LocalSearchPhaseConfig());
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        assertThat(solver.getPhaseList())
                .first()
                .isNotInstanceOf(PossiblyInitializingPhase.class);

        // CH - CH - LS
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig());
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isFalse();
        var secondPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(1);
        assertThat(secondPhase.isLastInitializingPhase()).isTrue();
        assertThat(solver.getPhaseList().get(2))
                .isNotInstanceOf(PossiblyInitializingPhase.class);

        // CP - CH - LS
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new NoChangeCustomPhaseCommand()),
                        new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig());
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isFalse();
        secondPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(1);
        assertThat(secondPhase.isLastInitializingPhase()).isTrue();
        assertThat(solver.getPhaseList().get(2))
                .isNotInstanceOf(PossiblyInitializingPhase.class);

        // CH - CP - LS
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(new NoChangeCustomPhaseCommand()),
                        new LocalSearchPhaseConfig());
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isFalse();
        secondPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(1);
        assertThat(secondPhase.isLastInitializingPhase()).isTrue();
        assertThat(solver.getPhaseList().get(2))
                .isNotInstanceOf(PossiblyInitializingPhase.class);

        // CP (CH) - CP (LS)
        solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(new NoChangeCustomPhaseCommand()),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(new NoChangeCustomPhaseCommand()));
        solverFactory = SolverFactory.create(solverConfig);
        solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        firstPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(0);
        assertThat(firstPhase.isLastInitializingPhase()).isFalse();
        secondPhase = (PossiblyInitializingPhase<TestdataSolution>) solver.getPhaseList().get(1);
        assertThat(secondPhase.isLastInitializingPhase()).isFalse();
    }

}
