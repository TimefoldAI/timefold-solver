package ai.timefold.solver.core.impl.move.streams.maybeapi.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListAssignMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListUnassignMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider.ListChangeMoveDefinition;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListChangeMoveDefinitionTest {

    @Test
    void fromSolution() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataListEntity.class)
                .planningListVariable();

        var solution = TestdataListSolution.generateUninitializedSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var unassignedValue = solution.getValueList().get(0);
        var initiallyAssignedValue = solution.getValueList().get(1);
        e2.getValueList().add(initiallyAssignedValue);
        solution.getEntityList().forEach(TestdataListEntity::setUpShadowVariables);

        var scoreDirector = createScoreDirector(solutionDescriptor, solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var moveDefinition = new ListChangeMoveDefinition<>(variableMetaModel);
        var moveProducer = moveDefinition.build(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(4);

        // Unassign moves are not generated, because the solution does not allow unassigned values.
        // Assign moves are generated for all three positions in e1 and e2.
        // Change move is generated for moving the initially assigned value from e2 to e1.

        var move1 = getListAssignMove(moveList, 0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move1.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move1.extractPlanningEntities())
                    .containsExactly(e1);
            softly.assertThat(move1.extractPlanningValues())
                    .containsExactly(unassignedValue);
        });

        var move2 = getListAssignMove(moveList, 1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move2.getDestinationIndex()).isEqualTo(1);
            softly.assertThat(move2.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move2.extractPlanningValues())
                    .containsExactly(unassignedValue);
        });

        var move3 = getListAssignMove(moveList, 2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move3.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move3.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move3.extractPlanningValues())
                    .containsExactly(unassignedValue);
        });

        var move4 = getListChangeMove(moveList, 3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move4.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move4.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move4.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move4.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move4.extractPlanningValues())
                    .containsExactly(initiallyAssignedValue);
        });
    }

    @Test
    void fromEntity() {
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataListEntityProvidingEntity.class)
                .planningListVariable();

        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var v3 = solution.getValueList().get(2);
        e1.getValueList().clear();
        var e2 = solution.getEntityList().get(1);
        var initiallyAssignedValue = e2.getValueRange().get(0);
        e2.getValueList().add(initiallyAssignedValue);
        solution.getEntityList().forEach(TestdataListEntityProvidingEntity::setUpShadowVariables);

        var scoreDirector =
                createScoreDirector(solutionDescriptor, solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var moveDefinition = new ListChangeMoveDefinition<>(variableMetaModel);
        var moveProducer = moveDefinition.build(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(4);

        // v1 can be moved from e2 to e1, because it's in the range for both.
        // v2 is unassigned; it can be assigned to e1, but not to e2.
        // v3 is unassigned; it can be assigned to e2, but not to e1.
        //                   e2 has one value already, and therefore two possible assignments, 0 and 1.

        var move1 = getListChangeMove(moveList, 0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move1.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move1.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move1.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move1.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move1.extractPlanningValues())
                    .containsExactly(v1);
        });

        var move2 = getListAssignMove(moveList, 1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move2.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move2.extractPlanningEntities())
                    .containsExactly(e1);
            softly.assertThat(move2.extractPlanningValues())
                    .containsExactly(v2);
        });

        var move3 = getListAssignMove(moveList, 2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move3.getDestinationIndex()).isEqualTo(1);
            softly.assertThat(move3.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move3.extractPlanningValues())
                    .containsExactly(v3);
        });

        var move4 = getListAssignMove(moveList, 3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move4.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move4.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move4.extractPlanningValues())
                    .containsExactly(v3);
        });
    }

    @Test
    void fromEntityAllowsUnassigned() {
        var solutionDescriptor = TestdataListUnassignedEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataListUnassignedEntityProvidingEntity.class)
                .planningListVariable();

        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var v3 = solution.getValueList().get(2);
        e2.getValueList().add(v1);

        var scoreDirector = createScoreDirector(solutionDescriptor,
                solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var moveDefinition = new ListChangeMoveDefinition<>(variableMetaModel);
        var moveProducer = moveDefinition.build(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(5);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(5);

        // v1 is assigned to e2, so it can be unassigned.
        // v1 can also be moved from e2 to e1, because it's in the range for both.
        // v2 is unassigned; it can be assigned to e1, but not to e2.
        // v3 is unassigned; it can be assigned to e2, but not to e1.
        //                   e2 has one value already, and therefore two possible assignments, 0 and 1.

        var move1 = getListUnassignMove(moveList, 0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move1.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move1.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move1.extractPlanningValues())
                    .containsExactly(v1);
        });

        var move2 = getListChangeMove(moveList, 1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move2.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move2.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move2.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move2.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move2.extractPlanningValues())
                    .containsExactly(v1);
        });

        var move3 = getListAssignMove(moveList, 2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move3.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move3.extractPlanningEntities())
                    .containsExactly(e1);
            softly.assertThat(move3.extractPlanningValues())
                    .containsExactly(v2);
        });

        var move4 = getListAssignMove(moveList, 3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move4.getDestinationIndex()).isEqualTo(1);
            softly.assertThat(move4.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move4.extractPlanningValues())
                    .containsExactly(v3);
        });

        var move5 = getListAssignMove(moveList, 4);
        assertSoftly(softly -> {
            softly.assertThat(move5.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move5.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move5.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move5.extractPlanningValues())
                    .containsExactly(v3);
        });
    }

    @Test
    void fromSolutionAllowsUnassigned() {
        var solutionDescriptor = TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataAllowsUnassignedValuesListEntity.class)
                .planningListVariable();
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e2.getValueList().add(v1);
        solution.getEntityList().forEach(TestdataAllowsUnassignedValuesListEntity::setUpShadowVariables);

        var scoreDirector = createScoreDirector(solutionDescriptor, solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var moveDefinition = new ListChangeMoveDefinition<>(variableMetaModel);
        var moveProducer = moveDefinition.build(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(5);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(5);

        // v1 is assigned to e2, so it can be unassigned.
        // v1 can also be moved from e2 to e1.
        // v2 is unassigned; it can be assigned to e1 or e2.
        //                   e2 has one value already, and therefore two possible assignments, 0 and 1.

        var move1 = getListUnassignMove(moveList, 0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move1.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move1.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move1.extractPlanningValues())
                    .containsExactly(v1);
        });

        var move2 = getListChangeMove(moveList, 1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move2.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move2.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move2.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move2.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move2.extractPlanningValues())
                    .containsExactly(v1);
        });

        var move3 = getListAssignMove(moveList, 2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move3.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move3.extractPlanningEntities())
                    .containsExactly(e1);
            softly.assertThat(move3.extractPlanningValues())
                    .containsExactly(v2);
        });

        var move4 = getListAssignMove(moveList, 3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move4.getDestinationIndex()).isEqualTo(1);
            softly.assertThat(move4.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move4.extractPlanningValues())
                    .containsExactly(v2);
        });

        var move5 = getListAssignMove(moveList, 4);
        assertSoftly(softly -> {
            softly.assertThat(move5.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move5.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move5.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move5.extractPlanningValues())
                    .containsExactly(v2);
        });
    }

    private static <Solution_, Entity_, Value_> ListUnassignMove<Solution_, Entity_, Value_>
            getListUnassignMove(List<Move<Solution_>> moveList, int index) {
        return (ListUnassignMove<Solution_, Entity_, Value_>) moveList.get(index);
    }

    private static <Solution_, Entity_, Value_> ListChangeMove<Solution_, Entity_, Value_>
            getListChangeMove(List<Move<Solution_>> moveList, int index) {
        return (ListChangeMove<Solution_, Entity_, Value_>) moveList.get(index);
    }

    private static <Solution_, Entity_, Value_> ListAssignMove<Solution_, Entity_, Value_>
            getListAssignMove(List<Move<Solution_>> moveList, int index) {
        return (ListAssignMove<Solution_, Entity_, Value_>) moveList.get(index);
    }

    private <Solution_> InnerScoreDirector<Solution_, ?> createScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution) {
        var constraintProvider = new TestingConstraintProvider(
                solutionDescriptor.getListVariableDescriptor().getEntityDescriptor().getEntityClass());
        var scoreDirectorFactory =
                new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider,
                        EnvironmentMode.TRACKED_FULL_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        return scoreDirector;
    }

    // The specifics of the constraint provider are not important for this test,
    // as the score will never be calculated.
    private record TestingConstraintProvider(Class<?> entityClass) implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
            return new Constraint[] { alwaysPenalizingConstraint(constraintFactory) };
        }

        private Constraint alwaysPenalizingConstraint(ConstraintFactory constraintFactory) {
            return constraintFactory.forEach(entityClass)
                    .penalize(SimpleScore.ONE)
                    .asConstraint("Always penalize");
        }

    }

    private <Solution_> MoveStreamSession<Solution_> createSession(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        var solution = scoreDirector.getWorkingSolution();
        var moveStreamSession = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        scoreDirector.getSolutionDescriptor().visitAll(solution, moveStreamSession::insert);
        moveStreamSession.settle();
        return moveStreamSession;
    }

}