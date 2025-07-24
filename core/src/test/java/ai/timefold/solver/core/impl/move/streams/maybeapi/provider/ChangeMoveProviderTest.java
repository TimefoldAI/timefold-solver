package ai.timefold.solver.core.impl.move.streams.maybeapi.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collections;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider.ChangeMoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedConstraintProvider;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingConstraintProvider;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingConstraintProvider;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeConstraintProvider;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeEntity;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeSolution;

import org.junit.jupiter.api.Test;

class ChangeMoveProviderTest {

    @Test
    void fromSolutionBasicVariable() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        var scoreDirector = createScoreDirector(solutionDescriptor, new TestdataConstraintProvider(), solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(secondMove.extractPlanningValues())
                    .containsExactly(secondValue);
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.extractPlanningValues())
                    .containsExactly(secondValue);
        });
    }

    @Test
    void fromSolutionBasicVariableIncompleteValueRange() {
        var solutionDescriptor = TestdataIncompleteValueRangeSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataIncompleteValueRangeEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();

        // The point of this test is to ensure that the move provider skips values that are not in the value range.
        var solution = TestdataIncompleteValueRangeSolution.generateSolution(2, 2);
        var valueNotInValueRange = new TestdataValue("third");
        solution.setValueListNotInValueRange(Collections.singletonList(valueNotInValueRange));

        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        var scoreDirector =
                createScoreDirector(solutionDescriptor, new TestdataIncompleteValueRangeConstraintProvider(), solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataIncompleteValueRangeSolution, TestdataIncompleteValueRangeEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(secondMove.extractPlanningValues())
                    .containsExactly(secondValue);
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.extractPlanningValues())
                    .containsExactly(secondValue);
        });
    }

    @Test
    void fromEntityBasicVariable() {
        var solutionDescriptor = TestdataEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataEntityProvidingEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();

        var solution = TestdataEntityProvidingSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = firstEntity.getValueRange().get(0);
        var scoreDirector = createScoreDirector(solutionDescriptor, new TestdataEntityProvidingConstraintProvider(), solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        // One move is expected:
        // - firstEntity is already assigned to firstValue, the only possible value; skip.
        // - Assign secondEntity to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(1);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataEntityProvidingSolution, TestdataEntityProvidingEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(firstMove.extractPlanningValues())
                    .hasSize(1)
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromEntityBasicVariableAllowsUnassigned() {
        var solutionDescriptor = TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();

        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = firstEntity.getValueRange().get(0);
        var scoreDirector = createScoreDirector(solutionDescriptor,
                new TestdataAllowsUnassignedEntityProvidingConstraintProvider(), solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        // Three moves are expected:
        // - Assign firstEntity to null,
        //   as it is currently assigned to firstValue, and the value range only contains firstValue.
        // - Assign secondEntity to null and to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        // Null is not in the value range, but as documented,
        // null is added automatically to value ranges when allowsUnassigned is true.
        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(3);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataAllowsUnassignedEntityProvidingSolution, TestdataAllowsUnassignedEntityProvidingEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(3);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.extractPlanningValues())
                    .hasSize(1)
                    .containsNull();
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(secondMove.extractPlanningValues())
                    .hasSize(1)
                    .containsNull();
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromSolutionBasicVariableAllowsUnassigned() {
        var solutionDescriptor = TestdataAllowsUnassignedSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataAllowsUnassignedEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();
        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0); // Assigned to null.
        var secondEntity = solution.getEntityList().get(1); // Assigned to secondValue.
        var firstValue = solution.getValueList().get(0); // Not assigned to any entity.
        var secondValue = solution.getValueList().get(1);
        var scoreDirector = createScoreDirector(solutionDescriptor, new TestdataAllowsUnassignedConstraintProvider(), solution);

        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);
        var moveStreamSession = createSession(moveStreamFactory, scoreDirector);

        // Filters out moves that would change the value to the value the entity already has.
        // Therefore this will have 4 moves (2 entities * 2 values) as opposed to 6 (2 entities * 3 values).
        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        // TODO There is a strange issue here that needs to be investigated,
        //   as it potentially breaks difficulty comparators.

        // The node network receives:
        // firstEntity + null; filtered out
        // secondEntity + null
        // firstEntity + firstValue
        // secondEntity + firstValue
        // firstEntity + secondValue
        // secondEntity + secondValue; filtered out

        // Therefore the iterator receives:
        // secondEntity + null
        // firstEntity + firstValue
        // secondEntity + firstValue
        // firstEntity + secondValue

        // This means that secondEntity is actually encountered first, and therefore will be iterated first.
        // A strange behavior of original iteration when combined with dataset caching before picking,
        // where the node network (= cache) is fully built long before the iteration starts.
        // A possible fix would be to refactor the node network to first iterate right inputs
        // (values first in this case)
        // but wouldn't that just create a similar issue in other places?

        // Second entity is assigned to secondValue, therefore the applicable moves assign to null and firstValue.
        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(firstMove.extractPlanningValues())
                    .containsExactly(new TestdataValue[] { null });
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(secondMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        // First entity is assigned to null, therefore the applicable moves assign to firstValue and secondValue.
        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(thirdMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.extractPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(fourthMove.extractPlanningValues())
                    .containsExactly(secondValue);
        });

    }

    private <Solution_> InnerScoreDirector<Solution_, ?> createScoreDirector(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintProvider constraintProvider, Solution_ solution) {
        var scoreDirectorFactory =
                new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider,
                        EnvironmentMode.TRACKED_FULL_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        return scoreDirector;
    }

    private <Solution_> MoveStreamSession<Solution_> createSession(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        var solution = scoreDirector.getWorkingSolution();
        var moveStreamSession = moveStreamFactory.createSession(new SessionContext<>(solution,
                scoreDirector.getValueRangeManager(), scoreDirector.getSupplyManager()));
        scoreDirector.getSolutionDescriptor().visitAll(solution, moveStreamSession::insert);
        moveStreamSession.settle();
        return moveStreamSession;
    }

}