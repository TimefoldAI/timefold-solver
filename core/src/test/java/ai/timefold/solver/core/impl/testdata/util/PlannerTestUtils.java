package ai.timefold.solver.core.impl.testdata.util;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.DummySimpleScoreEasyScoreCalculator;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.mockito.AdditionalAnswers;

/**
 * @see PlannerAssert
 */
public final class PlannerTestUtils {

    public static final int TERMINATION_STEP_COUNT_LIMIT = 10;

    // ************************************************************************
    // SolverFactory methods
    // ************************************************************************

    public static <Solution_> SolverFactory<Solution_> buildSolverFactory(
            Class<Solution_> solutionClass, Class<?>... entityClasses) {
        SolverConfig solverConfig = buildSolverConfig(solutionClass, entityClasses);
        return SolverFactory.create(solverConfig);
    }

    public static <Solution_> SolverConfig buildSolverConfig(Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        return new SolverConfig()
                .withSolutionClass(solutionClass)
                .withEntityClasses(entityClasses)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig().withTerminationConfig(
                                new TerminationConfig().withStepCountLimit(TERMINATION_STEP_COUNT_LIMIT)));
    }

    public static <Solution_> Solution_ solve(SolverConfig solverConfig, Solution_ problem) {
        return solve(solverConfig, problem, true);
    }

    public static <Solution_> Solution_ solve(SolverConfig solverConfig, Solution_ problem, boolean bestSolutionEventExists) {
        SolverFactory<Solution_> solverFactory = SolverFactory.create(solverConfig);
        Solver<Solution_> solver = solverFactory.buildSolver();
        AtomicReference<Solution_> eventBestSolutionRef = new AtomicReference<>();
        solver.addEventListener(event -> eventBestSolutionRef.set(event.getNewBestSolution()));
        Solution_ finalBestSolution = solver.solve(problem);
        if (bestSolutionEventExists) {
            assertThat(eventBestSolutionRef).doesNotHaveNullValue();
        } else {
            assertThat(eventBestSolutionRef).hasNullValue();
        }
        return finalBestSolution;
    }

    // ************************************************************************
    // Testdata methods
    // ************************************************************************

    public static TestdataSolution generateTestdataSolution(String code) {
        return generateTestdataSolution(code, 2);
    }

    public static TestdataSolution generateTestdataSolution(String code, int entityAndValueCount) {
        TestdataSolution solution = new TestdataSolution(code);
        solution.setValueList(IntStream.range(1, entityAndValueCount + 1)
                .mapToObj(i -> new TestdataValue("v" + i))
                .collect(Collectors.toList()));
        solution.setEntityList(IntStream.range(1, entityAndValueCount + 1)
                .mapToObj(i -> new TestdataEntity("e" + i))
                .collect(Collectors.toList()));
        return solution;
    }

    // ************************************************************************
    // ScoreDirector methods
    // ************************************************************************

    public static <Solution_> AbstractScoreDirector<Solution_, SimpleScore, ?> mockScoreDirector(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        EasyScoreDirectorFactory<Solution_, SimpleScore> scoreDirectorFactory =
                new EasyScoreDirectorFactory<>(solutionDescriptor, (solution_) -> SimpleScore.of(0));
        scoreDirectorFactory.setInitializingScoreTrend(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1));
        return mock(AbstractScoreDirector.class,
                AdditionalAnswers.delegatesTo(scoreDirectorFactory.buildScoreDirector(false, false)));
    }

    public static <Solution_, Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_>
            mockRebasingScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor, Object[][] lookUpMappings) {
        InnerScoreDirector<Solution_, Score_> scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);
        when(scoreDirector.lookUpWorkingObject(any())).thenAnswer((invocation) -> {
            Object externalObject = invocation.getArguments()[0];
            if (externalObject == null) {
                return null;
            }
            for (Object[] lookUpMapping : lookUpMappings) {
                if (externalObject == lookUpMapping[0]) {
                    return lookUpMapping[1];
                }
            }
            throw new IllegalStateException("No method mocked for parameter (" + externalObject + ").");
        });
        return scoreDirector;
    }

    // ************************************************************************
    // Collection helpers
    // ************************************************************************

    @SafeVarargs
    public static <X> Set<X> asSet(X... items) {
        return stream(items).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @SafeVarargs
    public static <X> SortedSet<X> asSortedSet(X... items) {
        return stream(items).collect(Collectors.toCollection(TreeSet::new));
    }

    public static <X, Y> Map<X, Y> asMap(X x1, Y y1) {
        Map<X, Y> result = new LinkedHashMap<>(0);
        result.put(x1, y1);
        return result;
    }

    public static <X, Y> Map<X, Y> asMap(X x1, Y y1, X x2, Y y2) {
        Map<X, Y> result = asMap(x1, y1);
        result.put(x2, y2);
        return result;
    }

    public static <X extends Comparable<X>, Y> SortedMap<X, Y> asSortedMap(X x1, Y y1) {
        SortedMap<X, Y> result = new TreeMap<>();
        result.put(x1, y1);
        return result;
    }

    public static <X extends Comparable<X>, Y> SortedMap<X, Y> asSortedMap(X x1, Y y1, X x2, Y y2) {
        SortedMap<X, Y> result = asSortedMap(x1, y1);
        result.put(x2, y2);
        return result;
    }

    // ************************************************************************
    // Scope helpers
    // ************************************************************************

    /**
     * Returns {@link AbstractPhaseScope} instance that will delegate to {@link SolverScope#getWorkingRandom()}.
     *
     * @param solverScope never null
     * @param <Solution_> generic type of the solution
     * @return never null
     */
    public static <Solution_> AbstractPhaseScope<Solution_> delegatingPhaseScope(SolverScope<Solution_> solverScope) {
        return new AbstractPhaseScope<>(solverScope, 0) {
            @Override
            public AbstractStepScope<Solution_> getLastCompletedStepScope() {
                return null;
            }
        };
    }

    /**
     * Returns {@link AbstractPhaseScope} instance that will delegate to {@link AbstractPhaseScope#getWorkingRandom()}.
     *
     * @param phaseScope never null
     * @param <Solution_> generic type of the solution
     * @return never null
     */
    public static <Solution_> AbstractStepScope<Solution_> delegatingStepScope(AbstractPhaseScope<Solution_> phaseScope) {
        return new AbstractStepScope<>(0) {
            @Override
            public AbstractPhaseScope<Solution_> getPhaseScope() {
                return phaseScope;
            }
        };
    }

    // ************************************************************************
    // Private constructor
    // ************************************************************************

    private PlannerTestUtils() {
    }

}
