package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListAssignMoveTest {

    private final InnerScoreDirector<TestdataListSolution, ?> scoreDirector = mock(AbstractScoreDirector.class);
    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @BeforeEach
    void setUp() {
        when(scoreDirector.getSolutionDescriptor())
                .thenReturn(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());
    }

    @Test
    void doMove() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListEntity e1 = new TestdataListEntity("e1");

        // v1 -> e1[0]
        ListAssignMove<TestdataListSolution> move = new ListAssignMove<>(variableDescriptor, v1, e1, 0);
        Move<TestdataListSolution> undoMove = move.doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 0, 0);
        verify(scoreDirector).beforeListVariableElementAssigned(variableDescriptor, v1);
        verify(scoreDirector).afterListVariableElementAssigned(variableDescriptor, v1);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 0, 1);
        verify(scoreDirector).triggerVariableListeners();
        verifyNoMoreInteractions(scoreDirector);

        // undo
        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).isEmpty();

        // v2 -> e1[0]
        new ListAssignMove<>(variableDescriptor, v2, e1, 0).doMove(scoreDirector);
        // v3 -> e1[1]
        new ListAssignMove<>(variableDescriptor, v3, e1, 1).doMove(scoreDirector);
        // v1 -> e1[0]
        new ListAssignMove<>(variableDescriptor, v1, e1, 0).doMove(scoreDirector);
        assertThat(e1.getValueList()).containsExactly(v1, v2, v3);
    }

    @Test
    void rebase() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListEntity e1 = new TestdataListEntity("e1");

        TestdataListValue destinationV1 = new TestdataListValue("1");
        TestdataListEntity destinationE1 = new TestdataListEntity("e1");

        ScoreDirector<TestdataListSolution> destinationScoreDirector = mockRebasingScoreDirector(
                variableDescriptor.getEntityDescriptor().getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { e1, destinationE1 },
                });

        assertSameProperties(
                destinationV1, destinationE1, 0,
                new ListAssignMove<>(variableDescriptor, v1, e1, 0).rebase(destinationScoreDirector));
    }

    static void assertSameProperties(
            Object movedValue, Object destinationEntity, int destinationIndex,
            ListAssignMove<?> move) {
        assertThat(move.getMovedValue()).isSameAs(movedValue);
        assertThat(move.getDestinationEntity()).isSameAs(destinationEntity);
        assertThat(move.getDestinationIndex()).isEqualTo(destinationIndex);
    }

    @Test
    void toStringTest() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListEntity e1 = new TestdataListEntity("E1");

        assertThat(new ListAssignMove<>(variableDescriptor, v1, e1, 15)).hasToString("1 {null -> E1[15]}");
    }
}
