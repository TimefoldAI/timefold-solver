package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListSwapMoveProviderTest {

    @Test
    void fromSolution() {
        var solutionDescriptor = TestdataListSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataListEntity.class)
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
        SolutionManager.updateShadowVariables(solution);

        var moveIterable = createMoveIterable(new ListSwapMoveProvider<>(variableMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();
        assertThat(moveList).hasSize(6);

        // We have 4 values.
        // One is unassigned, therefore isn't included in the swaps.
        // Three other values can be mutually swapped:
        // - assignedValue1 <-> assignedValue2
        // - assignedValue1 <-> assignedValue3
        // - assignedValue2 <-> assignedValue3
        // That makes 6 possible swap moves. (Includes duplicates.)

        var move1 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move1.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue2);
        });

        var move2 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move2.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue3);
        });

        var move3 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move3.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue2);
        });

        var move4 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getPlanningEntities())
                    .containsOnly(e2);
            softly.assertThat(move4.getPlanningValues())
                    .containsOnly(assignedValue2, assignedValue3);
        });

        var move5 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(4);
        assertSoftly(softly -> {
            softly.assertThat(move5.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move5.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue3);
        });

        var move6 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(5);
        assertSoftly(softly -> {
            softly.assertThat(move6.getPlanningEntities())
                    .containsOnly(e2);
            softly.assertThat(move6.getPlanningValues())
                    .containsOnly(assignedValue2, assignedValue3);
        });
    }

    @Test
    void fromEntity() {
        var solutionDescriptor = TestdataListEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataListEntityProvidingEntity.class)
                .listVariable();

        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        e1.getValueList().clear();
        var initiallyAssignedValue = e2.getValueRange().get(0);
        e2.getValueList().add(initiallyAssignedValue);
        SolutionManager.updateShadowVariables(solution);

        var moveIterable = createMoveIterable(new ListSwapMoveProvider<>(variableMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .toList();

        // There is only one overlapping value between the ranges of e1 and e2: v1.
        // Therefore there are no possible swap moves.
        assertThat(moveList).isEmpty();
    }

    private <Solution_> Iterable<Move<Solution_>> createMoveIterable(MoveProvider<Solution_> moveProvider,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution) {
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor, EnvironmentMode.TRACKED_FULL_ASSERT);
        var moveStream = moveProvider.build(moveStreamFactory);
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