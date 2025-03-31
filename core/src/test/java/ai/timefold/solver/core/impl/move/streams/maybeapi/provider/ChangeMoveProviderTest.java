package ai.timefold.solver.core.impl.move.streams.maybeapi.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.stream.StreamSupport;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.generic.provider.ChangeMoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamSession;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.Test;

class ChangeMoveProviderTest {

    @Test
    void fromSolution() {
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
        var moveStreamSession = createSession(moveStreamFactory, solutionDescriptor, solution);

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

    private MoveStreamSession<TestdataSolution> createSession(DefaultMoveStreamFactory<TestdataSolution> moveStreamFactory,
            SolutionDescriptor<TestdataSolution> solutionDescriptor, TestdataSolution solution) {
        var moveStreamSession = moveStreamFactory.createSession(solution);
        solutionDescriptor.visitAll(solution, moveStreamSession::insert);
        moveStreamSession.settle();
        return moveStreamSession;
    }

}
