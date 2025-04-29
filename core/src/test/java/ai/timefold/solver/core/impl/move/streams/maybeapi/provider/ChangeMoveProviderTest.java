package ai.timefold.solver.core.impl.move.streams.maybeapi.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.generic.provider.ChangeMoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;

import org.junit.jupiter.api.Test;

class ChangeMoveProviderTest {

    @Test
    void fromSolutionBasicVariable() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        var moveStreamSession = createSession(moveStreamFactory, solutionDescriptor, solution,
                mock(SupplyManager.class));

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
    void fromSolutionBasicVariableAllowsUnassigned() {
        var solutionDescriptor = TestdataAllowsUnassignedSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .entity(TestdataAllowsUnassignedEntity.class)
                .genuineVariable()
                .ensurePlanningVariable();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(solutionDescriptor);
        var moveProvider = new ChangeMoveProvider<>(variableMetaModel);
        var moveProducer = moveProvider.apply(moveStreamFactory);

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0); // Assigned to null.
        var secondEntity = solution.getEntityList().get(1); // Assigned to secondValue.
        var firstValue = solution.getValueList().get(0); // Not assigned to any entity.
        var secondValue = solution.getValueList().get(1);
        var moveStreamSession = createSession(moveStreamFactory, solutionDescriptor, solution,
                mock(SupplyManager.class));

        // Filters out moves that would change the value to the value the entity already has.
        // Therefore this will have 4 moves (2 entities * 2 values) as opposed to 6 (2 entities * 3 values).
        var moveIterable = moveProducer.getMoveIterable(moveStreamSession);
        assertThat(moveIterable).hasSize(4);

        var moveList = StreamSupport.stream(moveIterable.spliterator(), false)
                .map(m -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) m)
                .toList();
        assertThat(moveList).hasSize(4);

        // First entity is assigned to null, therefore the applicable moves assign to firstValue and secondValue.
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

        // Second entity is assigned to secondValue, therefore the applicable moves assign to null and firstValue.
        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.extractPlanningValues())
                    .containsExactly(new TestdataValue[] { null });
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.extractPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.extractPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    private <Solution_> MoveStreamSession<Solution_> createSession(DefaultMoveStreamFactory<Solution_> moveStreamFactory,
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution, SupplyManager supplyManager) {
        var moveStreamSession = moveStreamFactory.createSession(solution, supplyManager);
        solutionDescriptor.visitAll(solution, moveStreamSession::insert);
        moveStreamSession.settle();
        return moveStreamSession;
    }

}
