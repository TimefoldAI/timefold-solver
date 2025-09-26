package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListSwapMoveDefinitionTest {

    @Test
    void fromSolution() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataListEntity.class)
                .listVariable();

        var solution = TestdataListSolution.generateUninitializedSolution(4, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var assignedValue1 = solution.getValueList().get(1);
        var assignedValue2 = solution.getValueList().get(2);
        var assignedValue3 = solution.getValueList().get(3);
        e1.getValueList().add(assignedValue1);
        e2.getValueList().add(assignedValue2);
        e2.getValueList().add(assignedValue3);
        solution.getEntityList().forEach(TestdataListEntity::setUpShadowVariables);

        var moveIterable = createMoveIterable(new ListSwapMoveDefinition<>(variableMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(3);

        // We have 4 values.
        // One is unassigned, therefore isn't included in the swaps.
        // Three other values can be mutually swapped:
        // - assignedValue1 <-> assignedValue2
        // - assignedValue1 <-> assignedValue3
        // - assignedValue2 <-> assignedValue3
        // That makes 3 possible swap moves.

        var move1 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move1.extractPlanningValues())
                    .containsExactly(assignedValue2, assignedValue1);
        });

        var move2 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.extractPlanningEntities())
                    .containsExactly(e2, e1);
            softly.assertThat(move2.extractPlanningValues())
                    .containsExactly(assignedValue3, assignedValue1);
        });

        var move3 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.extractPlanningEntities())
                    .containsExactly(e2);
            softly.assertThat(move3.extractPlanningValues())
                    .containsExactly(assignedValue3, assignedValue2);
        });
    }

    @Test
    void fromEntity() {
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataListEntityProvidingEntity.class)
                .listVariable();

        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        e1.getValueList().clear();
        var initiallyAssignedValue = e2.getValueRange().get(0);
        e2.getValueList().add(initiallyAssignedValue);
        solution.getEntityList().forEach(TestdataListEntityProvidingEntity::setUpShadowVariables);

        var moveIterable = createMoveIterable(new ListSwapMoveDefinition<>(variableMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();

        // There is only one overlapping value between the ranges of e1 and e2: v1.
        // Therefore there are no possible swap moves.
        assertThat(moveList).isEmpty();
    }

    private <Solution_> Iterable<Move<Solution_>> createMoveIterable(MoveDefinition<Solution_> moveDefinition,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.TRACKED_FULL_ASSERT);
        var moveStream = moveDefinition.build(moveStreamFactory);
        var scoreDirector = createScoreDirector(solutionDescriptor, solution);
        var neighborhoodSession = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        solutionDescriptor.visitAll(scoreDirector.getWorkingSolution(), neighborhoodSession::insert);
        neighborhoodSession.settle();
        return moveStream.getMoveIterable(neighborhoodSession);
    }

    private <Solution_> InnerScoreDirector<Solution_, ?> createScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution) {
        var firstEntityClass = solutionDescriptor.getMetaModel().genuineEntities().get(0).type();
        var constraintProvider = new TestingConstraintProvider(firstEntityClass);
        var scoreDirectorFactory = new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider,
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

}