package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListUnassignMoveTest {

    @Test
    void doMove() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = new TestdataListEntity("e1", v1, v2, v3);

        var scoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) mock(AbstractScoreDirector.class);
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();

        // Unassign last
        var move = new ListUnassignMove<>(variableDescriptor, e1, 2);
        var undoMove = move.doMove(scoreDirector);
        assertThat(undoMove).isNotNull();
        assertThat(e1.getValueList()).containsExactly(v1, v2);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, e1, 2, 3);
        verify(scoreDirector).beforeListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, e1, 2, 2);
        verify(scoreDirector).triggerVariableListeners();
        verifyNoMoreInteractions(scoreDirector);

        // Unassign the rest
        new ListUnassignMove<>(variableDescriptor, e1, 0).doMoveOnly(scoreDirector);
        new ListUnassignMove<>(variableDescriptor, e1, 0).doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).isEmpty();
    }

    @Test
    void undoMove() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = new TestdataListEntity("e1", v1, v2, v3);

        var scoreDirector = (InnerScoreDirector<TestdataListSolution, SimpleScore>) mock(AbstractScoreDirector.class);
        var variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();

        var move = new ListUnassignMove<>(variableDescriptor, e1, 2);
        var undoMove = move.doMove(scoreDirector);
        assertThat(e1.getValueList()).hasSize(2);
        verify(scoreDirector).beforeListVariableElementUnassigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableElementUnassigned(variableDescriptor, v3);

        undoMove.doMoveOnly(scoreDirector);
        assertThat(e1.getValueList()).hasSize(3);
        verify(scoreDirector).beforeListVariableElementAssigned(variableDescriptor, v3);
        verify(scoreDirector).afterListVariableElementAssigned(variableDescriptor, v3);
    }

    @Test
    void rebase() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var variableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var destinationScoreDirector = mockRebasingScoreDirector(solutionDescriptor, new Object[][] {});
        var move = new ListUnassignMove<TestdataSolution>(null, null, 0);
        var rebasedMove = move.rebase(destinationScoreDirector);
        assertThat(rebasedMove).isNotSameAs(move);
    }

    @Test
    void toStringTest() {
        var v1 = new TestdataListValue("1");
        var e1 = TestdataListEntity.createWithValues("E1", v1);

        var variableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();

        assertThat(new ListUnassignMove<>(variableDescriptor, e1, 0)).hasToString("1 {E1[0] -> null}");
    }
}
