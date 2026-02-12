package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhase;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AcceptorFactory;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForagerFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.ChangeMoveProvider;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.builtin.SwapMove;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListEntity;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class NeighborhoodsTest {

    @Test
    void changeMoveBasedLocalSearch() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var heuristicConfigPolicy =
                new HeuristicConfigPolicy.Builder<TestdataSolution>().withSolutionDescriptor(solutionDescriptor).build();
        var termination = (PhaseTermination<TestdataSolution>) TerminationFactory
                .<TestdataSolution> create(new TerminationConfig().withBestScoreLimit("0")) // All entities are assigned to a particular value.
                .buildTermination(heuristicConfigPolicy);

        var variableMetaModel = solutionDescriptor.getMetaModel().genuineEntity(TestdataEntity.class).basicVariable();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        // Random selection otherwise LS gets stuck in an endless loop.
        var moveRepository = new NeighborhoodsBasedMoveRepository<>(moveStreamFactory,
                List.of(new ChangeMoveProvider<>(variableMetaModel)), true);

        var acceptor = AcceptorFactory.<TestdataSolution> create(new LocalSearchAcceptorConfig().withLateAcceptanceSize(400))
                .buildAcceptor(heuristicConfigPolicy);
        var forager = LocalSearchForagerFactory
                .<TestdataSolution> create(new LocalSearchForagerConfig().withAcceptedCountLimit(1)).buildForager();
        var localSearchDecider = new LocalSearchDecider<>("", termination, moveRepository, acceptor, forager);
        var localSearchPhase = new DefaultLocalSearchPhase.Builder<>(0, "", termination, localSearchDecider).build();

        // Generates a solution whose entities' values are all set to the second value.
        // The easy calculator penalizes this.
        // The goal of the solver is to get all the entities to be assigned to the first value.
        var solution = TestdataSolution.generateSolution(2, 5);
        var secondValue = solution.getValueList().get(1);
        solution.getEntityList().forEach(e -> e.setValue(secondValue));

        var scoreDirector =
                new EasyScoreDirectorFactory<>(solutionDescriptor, new TestingEasyScoreCalculator(),
                        EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
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
        assertThatCode(() -> localSearchPhase.solve(solverScope)).doesNotThrowAnyException();
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

    @Test
    void allowsNullValues() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable("value",
                TestdataValue.class);

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 3);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(solution.getValueList().get(0));
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var unchangingAssignedEntity = solution.getEntityList().get(2);
        firstEntity.setValue(solution.getValueList().get(1));

        // The above solution, together with the move provider below, should generate 2 swap moves:
        // - firstEntity <-> secondEntity
        // - unchangingAssignedEntity <-> secondEntity
        var context = NeighborhoodTester.build(new SwapAssignedAndUnassigned(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moveList =
                context.getMovesAsList(m -> (SwapMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity>) m);
        assertThat(moveList).hasSize(2);
        var move1 = moveList.get(0);
        assertSoftly(softly -> {
            assertThat(move1.getPlanningEntities())
                    .containsExactly(firstEntity, secondEntity);
            assertThat(move1.getPlanningValues())
                    .containsExactly(firstEntity.getValue(), secondEntity.getValue());
        });
        var move2 = moveList.get(1);
        assertSoftly(softly -> {
            assertThat(move2.getPlanningEntities())
                    .containsExactly(unchangingAssignedEntity, secondEntity);
            assertThat(move2.getPlanningValues())
                    .containsExactly(unchangingAssignedEntity.getValue(), secondEntity.getValue());
        });

        // Execute one swap and verify the new moves:
        // - unchangingAssignedEntity <-> firstEntity
        // - secondEntity <-> firstEntity
        context.getMoveTestContext().execute(move1);
        moveList = context.getMovesAsList(m -> (SwapMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity>) m);
        assertThat(moveList).hasSize(2);
        var move3 = moveList.get(0);
        assertSoftly(softly -> {
            assertThat(move3.getPlanningEntities())
                    .containsExactly(unchangingAssignedEntity, firstEntity);
            assertThat(move3.getPlanningValues())
                    .containsExactly(unchangingAssignedEntity.getValue(), firstEntity.getValue());
        });
        var move4 = moveList.get(1);
        assertSoftly(softly -> {
            assertThat(move4.getPlanningEntities())
                    .containsExactly(secondEntity, firstEntity);
            assertThat(move4.getPlanningValues())
                    .containsExactly(secondEntity.getValue(), firstEntity.getValue());
        });

    }

    private record SwapAssignedAndUnassigned(
            PlanningVariableMetaModel<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataAllowsUnassignedSolution> {

        @Override
        public MoveStream<TestdataAllowsUnassignedSolution>
                build(MoveStreamFactory<TestdataAllowsUnassignedSolution> moveStreamFactory) {
            var assignedEntity = moveStreamFactory.forEach(TestdataAllowsUnassignedEntity.class, false)
                    .filter((solutionView, entity) -> entity.getValue() != null);
            var unassignedEntity = moveStreamFactory.forEach(TestdataAllowsUnassignedEntity.class, false)
                    .filter((solutionView, entity) -> entity.getValue() == null);

            return moveStreamFactory.pick(assignedEntity)
                    .pick(unassignedEntity)
                    .asMove((schedule, assigned, unassigned) -> Moves.swap(variable, assigned, unassigned));
        }
    }

    @Test
    void notifyWhenValueIndexChangesOnSimpleModel() {
        var solution = new TestdataDeclarativeSimpleListSolution();
        var entity1 = new TestdataDeclarativeSimpleListEntity("e1", 0, 0);
        var entity2 = new TestdataDeclarativeSimpleListEntity("e2", 0, 0);

        var value1 = new TestdataDeclarativeSimpleListValue("v1", 0, 0);
        var value2 = new TestdataDeclarativeSimpleListValue("v2", 0, 0);
        var value3 = new TestdataDeclarativeSimpleListValue("v3", 0, 0);

        entity1.getValues().add(value1);
        entity2.getValues().add(value2);
        entity2.getValues().add(value3);

        solution.setEntityList(List.of(entity1, entity2));
        solution.setValueList(List.of(value1, value2, value3));

        var neighborhoodMoveRepository = mock(NeighborhoodsBasedMoveRepository.class);
        var solutionDescriptor = TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor();
        var solutionMetamodel = solutionDescriptor.getMetaModel();
        var variableMetamodel = solutionMetamodel.genuineEntity(TestdataDeclarativeSimpleListEntity.class)
                .listVariable("values", TestdataDeclarativeSimpleListValue.class);
        try (var scoreDirector = new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor,
                constraintFactory -> new Constraint[] {
                        constraintFactory.forEach(Object.class).penalize(SimpleScore.ONE).asConstraint("dummy constraint") },
                EnvironmentMode.FULL_ASSERT).buildScoreDirector()) {
            scoreDirector.setMoveRepository(neighborhoodMoveRepository);
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.calculateScore();

            var move = Moves.change(variableMetamodel, ElementPosition.of(entity1, 0), ElementPosition.of(entity2, 0));
            scoreDirector.executeMove(move);
            scoreDirector.calculateScore();
        }

        verify(neighborhoodMoveRepository, atLeastOnce()).update(value1);
        verify(neighborhoodMoveRepository, atLeastOnce()).update(value2);
        verify(neighborhoodMoveRepository, atLeastOnce()).update(value3);
    }

}
