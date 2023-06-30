package ai.timefold.solver.examples.common.app;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.examples.common.TestSystemProperties;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class SolveAllTurtleTest<Solution_> extends LoggingTest {

    interface ProblemFactory<Solution_> extends Function<File, Solution_> {

        default Solution_ loadProblem(File f) {
            return apply(f);
        }
    }

    private static final String MOVE_THREAD_COUNT_OVERRIDE = System.getProperty(TestSystemProperties.MOVE_THREAD_COUNT);

    protected abstract List<File> getSolutionFiles(CommonApp<Solution_> commonApp);

    protected abstract CommonApp<Solution_> createCommonApp();

    protected abstract ProblemFactory<Solution_> createProblemFactory(CommonApp<Solution_> commonApp);

    private List<File> getFilteredSolutionFiles(CommonApp<Solution_> commonApp) {
        List<File> solutionFiles = getSolutionFiles(commonApp);
        String propertyValue = System.getProperty(TestSystemProperties.TURTLE_TEST_RUN_TIME_LIMIT);
        if (propertyValue == null) {
            logger.info("Will run all tests due to no time limit being set.");
            return solutionFiles;
        }
        int availableMinutes = Integer.parseInt(propertyValue);
        int testCount = solutionFiles.size();
        int maximumTestCount = (int) Math.floor(availableMinutes / 10.0); // One test will take 10 minutes to run.
        if (testCount <= maximumTestCount) {
            logger.info("Will run all tests as they all fit within the time limit of {} minutes.", availableMinutes);
            return solutionFiles;
        }
        long seed = System.nanoTime();
        logger.info("Will randomly filter out some tests to fit within the time window. Using random seed ({}).", seed);
        Random random = new Random(seed);
        double ratioOfTestsToRun = maximumTestCount / (double) testCount;
        Map<File, List<File>> testsPerParentDirectoryMap = solutionFiles.stream()
                .collect(Collectors.groupingBy(File::getParentFile, Collectors.toList()));
        List<File> filteredSolutionFiles = new ArrayList<>(maximumTestCount);
        // Make sure that each directory gets at least one test.
        for (List<File> testsInDirectory : testsPerParentDirectoryMap.values()) {
            int testsToRunInDirectory = (int) Math.max(1, testsInDirectory.size() * ratioOfTestsToRun);
            for (int i = 0; i < testsToRunInDirectory; i++) {
                int fileIndex = random.nextInt(testsInDirectory.size());
                filteredSolutionFiles.add(testsInDirectory.remove(fileIndex));
            }
        }
        // If we are still over the limit, remove random tests.
        while (filteredSolutionFiles.size() > maximumTestCount) {
            int fileIndex = random.nextInt(filteredSolutionFiles.size());
            filteredSolutionFiles.remove(fileIndex);
        }
        logger.info("Filtered out ({}) out of ({}) tests to run in ({}) minutes.",
                testCount - filteredSolutionFiles.size(), testCount, availableMinutes);
        return filteredSolutionFiles;
    }

    @Execution(ExecutionMode.CONCURRENT)
    @TestFactory
    Stream<DynamicTest> runFastAndFullAssert() {
        CommonApp<Solution_> commonApp = createCommonApp();
        ProblemFactory<Solution_> problemFactory = createProblemFactory(commonApp);
        /*
         * When run in Github Actions, we are limited by the maximum amount of time that the job can run for.
         * This code intends to limit the number of tests so that they can all run within the time limit.
         */
        return getFilteredSolutionFiles(commonApp).stream()
                .map(solutionFile -> dynamicTest(solutionFile.getName(), () -> runFastAndFullAssert(
                        buildSolverConfig(commonApp.getSolverConfigResource()),
                        problemFactory.loadProblem(solutionFile))));
    }

    public void runFastAndFullAssert(SolverConfig solverConfig, Solution_ problem) {
        // Specifically use NON_INTRUSIVE_FULL_ASSERT instead of FULL_ASSERT to flush out bugs hidden by intrusiveness
        // 1) NON_INTRUSIVE_FULL_ASSERT ASSERT to find CH bugs (but covers little ground)
        problem = buildAndSolve(solverConfig, EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT, problem, 2L);
        // 2) FAST_ASSERT to run past CH into LS to find easy bugs (but covers much ground)
        problem = buildAndSolve(solverConfig, EnvironmentMode.FAST_ASSERT, problem, 5L);
        // 3) NON_INTRUSIVE_FULL_ASSERT ASSERT to find LS bugs (but covers little ground)
        buildAndSolve(solverConfig, EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT, problem, 3L);
    }

    private static SolverConfig buildSolverConfig(String solverConfigResource) {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        // buildAndSolve() fills in minutesSpentLimit
        solverConfig.setTerminationConfig(new TerminationConfig());
        if (MOVE_THREAD_COUNT_OVERRIDE != null) {
            solverConfig.setMoveThreadCount(MOVE_THREAD_COUNT_OVERRIDE);
        }
        return solverConfig;
    }

    private Solution_ buildAndSolve(SolverConfig solverConfig, EnvironmentMode environmentMode,
            Solution_ problem, long maximumMinutesSpent) {
        solverConfig.getTerminationConfig().setMinutesSpentLimit(maximumMinutesSpent);
        solverConfig.setEnvironmentMode(environmentMode);
        Class<? extends EasyScoreCalculator> easyScoreCalculatorClass = overwritingEasyScoreCalculatorClass();
        if (easyScoreCalculatorClass != null && environmentMode.isAsserted()) {
            ScoreDirectorFactoryConfig assertionScoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
            assertionScoreDirectorFactoryConfig.setEasyScoreCalculatorClass(easyScoreCalculatorClass);
            solverConfig.getScoreDirectorFactoryConfig().setAssertionScoreDirectorFactory(
                    assertionScoreDirectorFactoryConfig);
        }
        SolverFactory<Solution_> solverFactory = SolverFactory.create(solverConfig);
        Solver<Solution_> solver = solverFactory.buildSolver();
        return solver.solve(problem);
    }

    protected Class<? extends EasyScoreCalculator> overwritingEasyScoreCalculatorClass() {
        return null;
    }
}
