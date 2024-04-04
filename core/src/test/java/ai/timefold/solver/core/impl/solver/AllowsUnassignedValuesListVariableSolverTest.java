package ai.timefold.solver.core.impl.solver;

import java.util.stream.IntStream;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Execution(ExecutionMode.CONCURRENT)
class AllowsUnassignedValuesListVariableSolverTest {

    @ParameterizedTest
    @EnumSource
    void runSolver(ListVariableMoveType moveType) {
        // Generate solution.
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(IntStream.range(0, 5)
                .mapToObj(i -> new TestdataAllowsUnassignedValuesListEntity("e" + i))
                .toList());
        solution.setValueList(IntStream.range(0, 25)
                .mapToObj(i -> new TestdataAllowsUnassignedValuesListValue("v" + i))
                .toList());

        // Generate deterministic, fully asserted solver.
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withSolutionClass(TestdataAllowsUnassignedValuesListSolution.class)
                .withEntityClasses(TestdataAllowsUnassignedValuesListEntity.class,
                        TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig()
                                .withAcceptorConfig(new LocalSearchAcceptorConfig()
                                        .withEntityTabuSize(1)
                                        .withValueTabuSize(1)
                                        .withMoveTabuSize(1))
                                .withForagerConfig(new LocalSearchForagerConfig()
                                        .withAcceptedCountLimit(100))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(1000))
                                .withMoveSelectorConfig(new UnionMoveSelectorConfig()
                                        .withMoveSelectors(moveType.moveSelectorConfigs)));
        var solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        // Run solver.
        var bestSolution = solver.solve(solution);
        Assertions.assertThat(bestSolution).isNotNull();
    }

    enum ListVariableMoveType {

        CHANGE_AND_SWAP(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig()),
        CHANGE_SWAP_AND_SUBLIST_CHANGE(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig(),
                new SubListChangeMoveSelectorConfig()),
        CHANGE_SWAP_AND_SUBLIST_SWAP(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig(),
                new SubListSwapMoveSelectorConfig()),
        KOPT(new KOptListMoveSelectorConfig()
                .withMinimumK(2)
                .withMaximumK(3)),
        CHANGE_SWAP_AND_KOPT(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig(),
                new KOptListMoveSelectorConfig()
                        .withMinimumK(2)
                        .withMaximumK(3)),
        ALL(new ChangeMoveSelectorConfig(),
                new SwapMoveSelectorConfig(),
                new SubListChangeMoveSelectorConfig(),
                new SubListSwapMoveSelectorConfig(),
                new KOptListMoveSelectorConfig()
                        .withMinimumK(2)
                        .withMaximumK(3));

        private final MoveSelectorConfig<?>[] moveSelectorConfigs;

        ListVariableMoveType(MoveSelectorConfig<?>... moveSelectorConfigs) {
            this.moveSelectorConfigs = moveSelectorConfigs;
        }

    }

}
