package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SwapMoveProviderTest {

    @Test
    void univariate() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var entityMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataEntity.class);

        var solution = TestdataSolution.generateSolution(2, 3);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var e3 = solution.getEntityList().get(2);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // But we only have 2 values, guaranteeing that two entities will share a value.
        // Therefore there will only be 4 swap moves (including duplicates).
        var moveIterable = createMoveIterable(new SwapMoveProvider<>(entityMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (SwapMove<TestdataSolution, TestdataEntity>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        var move1 = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move1.getPlanningValues())
                    .containsOnly(v1, v2);
        });

        var move2 = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move2.getPlanningValues())
                    .containsOnly(v1, v2);
        });

        var move3 = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getPlanningEntities())
                    .containsOnly(e2, e3);
            softly.assertThat(move3.getPlanningValues())
                    .containsOnly(v2, v1);
        });

        var move4 = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getPlanningEntities())
                    .containsOnly(e2, e3);
            softly.assertThat(move4.getPlanningValues())
                    .containsOnly(v2, v1);
        });

    }

    @Test
    void multivariate() {
        var solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        var entityMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataMultiVarEntity.class);

        var solution = TestdataMultiVarSolution.generateSolution(3, 2, 2);
        var e1 = solution.getMultiVarEntityList().get(0);
        var e2 = solution.getMultiVarEntityList().get(1);
        var e3 = solution.getMultiVarEntityList().get(2);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var otherV1 = solution.getOtherValueList().get(0);
        var otherV2 = solution.getOtherValueList().get(1);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // But we only have 2 unique combinations of values, guaranteeing that two entities will share values.
        // Therefore there will only be 4 swap moves (including duplicates).
        var moveIterable = createMoveIterable(new SwapMoveProvider<>(entityMetaModel), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (SwapMove<TestdataMultiVarSolution, TestdataMultiVarEntity>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        var move1 = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move1.getPlanningValues())
                    .containsOnly(v1, v2, otherV1, otherV2);
        });

        var move2 = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move2.getPlanningValues())
                    .containsOnly(v1, v2, otherV1, otherV2);
        });

        var move3 = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getPlanningEntities())
                    .containsOnly(e2, e3);
            softly.assertThat(move3.getPlanningValues())
                    .containsOnly(v2, v1, otherV2, otherV1);
        });

        var move4 = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getPlanningEntities())
                    .containsOnly(e2, e3);
            softly.assertThat(move4.getPlanningValues())
                    .containsOnly(v2, v1, otherV2, otherV1);
        });
    }

    @Test
    void multivariateWithExclusions() {
        var solutionDescriptor = TestdataMultiVarSolution.buildSolutionDescriptor();
        var entityMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataMultiVarEntity.class);
        var allowedVariableMetaModels = entityMetaModel.variables().stream()
                .filter(v -> !v.name().contains("tertiary"))
                .map(v -> (PlanningVariableMetaModel<TestdataMultiVarSolution, TestdataMultiVarEntity, Object>) v)
                .toList();
        var solution = TestdataMultiVarSolution.generateSolution(3, 1, 2);

        // With 3 entities, only 3 swap moves are possible: e1 <-> e2, e1 <-> e3, e2 <-> e3.
        // We only have 1 value for primary and secondary variables,
        // therefore with the tertiary variable excluded, there will be no swap moves.
        var moveIterable =
                createMoveIterable(new SwapMoveProvider<>(allowedVariableMetaModels), solutionDescriptor, solution);
        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (SwapMove<TestdataMultiVarSolution, TestdataMultiVarEntity>) m)
                .toList();
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