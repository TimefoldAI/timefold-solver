package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhase;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AcceptorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForagerFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class NeighborhoodsBasedLocalSearchTest {

    @Test
    void changeMoveBasedLocalSearch() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var heuristicConfigPolicy = new HeuristicConfigPolicy.Builder<TestdataSolution>()
                .withSolutionDescriptor(solutionDescriptor)
                .build();
        var termination = (PhaseTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(new TerminationConfig()
                        .withBestScoreLimit("0")) // All entities are assigned to a particular value.
                .buildTermination(heuristicConfigPolicy);
        var moveRepository = getMoveRepository(solutionDescriptor);
        var acceptor = AcceptorFactory.<TestdataSolution> create(new LocalSearchAcceptorConfig()
                .withLateAcceptanceSize(400))
                .buildAcceptor(heuristicConfigPolicy);
        var forager = LocalSearchForagerFactory.<TestdataSolution> create(new LocalSearchForagerConfig()
                .withAcceptedCountLimit(1))
                .buildForager();
        var localSearchDecider = new LocalSearchDecider<>("", termination, moveRepository, acceptor, forager);
        var localSearchPhase = new DefaultLocalSearchPhase.Builder<>(0, "", termination, localSearchDecider)
                .build();

        // Generates a solution whose entities' values are all set to the second value.
        // The easy calculator penalizes this.
        // The goal of the solver is to get all the entities to be assigned to the first value.
        var solution = TestdataSolution.generateSolution(2, 5);
        var secondValue = solution.getValueList().get(1);
        solution.getEntityList().forEach(e -> e.setValue(secondValue));

        var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor, new TestingEasyScoreCalculator())
                .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var score = scoreDirector.calculateScore();

        var bestSolutionRecaller = new BestSolutionRecaller<TestdataSolution>();
        var solver = mock(AbstractSolver.class);
        doReturn(List.of(localSearchPhase)).when(solver).getPhaseList();
        doReturn(bestSolutionRecaller).when(solver).getBestSolutionRecaller();
        var solverEventSupport = new SolverEventSupport<TestdataSolution>(solver);
        bestSolutionRecaller.setSolverEventSupport(solverEventSupport);
        var solverScope = new SolverScope<TestdataSolution>();
        solverScope.setSolver(solver);
        solverScope.setWorkingRandom(new Random());
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setBestScore(score);
        solverScope.setBestSolution(scoreDirector.cloneSolution(solution));
        solverScope.setProblemSizeStatistics(scoreDirector.getValueRangeManager().getProblemSizeStatistics());
        solverScope.startingNow();

        bestSolutionRecaller.solvingStarted(solverScope);
        assertThatCode(() -> localSearchPhase.solve(solverScope))
                .doesNotThrowAnyException();
    }

    private static NeighborhoodsBasedMoveRepository<TestdataSolution>
            getMoveRepository(SolutionDescriptor<TestdataSolution> solutionDescriptor) {
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataEntity.class)
                .basicVariable();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        // Random selection otherwise LS gets stuck in an endless loop.
        return new NeighborhoodsBasedMoveRepository<>(moveStreamFactory,
                List.of(new ChangeMoveProvider<>(variableMetaModel)), true);
    }

    /**
     * Penalizes the number of values which are not the first value.
     */
    private static final class TestingEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            var valueList = testdataSolution.getValueList();
            var firstValue = valueList.get(0);
            var valueSet = new HashSet<TestdataValue>(valueList.size());
            testdataSolution.getEntityList().forEach(e -> {
                if (e.getValue() != firstValue) {
                    valueSet.add(e.getValue());
                }
            });
            return SimpleScore.of(-valueSet.size());
        }

    }

}
